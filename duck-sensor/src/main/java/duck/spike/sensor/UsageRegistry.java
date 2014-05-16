package duck.spike.sensor;

import duck.spike.util.Configuration;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.runtime.reflect.Factory;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Olle Hallin
 */
public class UsageRegistry {

    private static class Usage {
        private final static Pattern PATTERN = Pattern.compile("^\\s*([\\d]+):(.*)");

        private final String signature;
        private final long usedAtMillis;

        private Usage(String signature, long usedAtMillis) {
            this.signature = signature;
            this.usedAtMillis = usedAtMillis;
        }

        @Override
        public String toString() {
            return String.format("%14d:%s", usedAtMillis, signature);
        }

        public static Usage parse(String line) {
            Matcher m = PATTERN.matcher(line);
            return m.matches() ? new Usage(m.group(2), Long.parseLong(m.group(1))) : null;
        }
    }

    private static final String MY_NAME = UsageRegistry.class.getName();

    private UsageRegistry() {
        // Utility class with only static methods
    }

    private static Configuration config;
    private static ConcurrentMap<String, Usage> trackedMethods = new ConcurrentHashMap<String, Usage>();
    private static AtomicBoolean classpathScanned = new AtomicBoolean(false);
    private static AtomicInteger dumpCount = new AtomicInteger();

    public static void initialize(Configuration config) {
        UsageRegistry.config = config;

        File parent = config.getDataFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (config.getDataFile().exists()) {
            // Continue from previous JVM run...
            initializeTrackedMethodsFrom(config.getDataFile());
        }
    }

    private static void initializeTrackedMethodsFrom(File file) {
        System.err.printf("%s: Found %s, will continue from that%n", MY_NAME, file.getAbsolutePath());

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = in.readLine()) != null) {
                Usage usage = Usage.parse(line);
                if (usage != null) {
                    trackedMethods.put(usage.signature, usage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * This method is invoked by {@link duck.spike.sensor.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain type and method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public static void registerMethodExecution(Signature signature) {
        Usage usage = new Usage(makeMethodKey(signature), System.currentTimeMillis());
        trackedMethods.put(usage.signature, usage);
    }

    private static String makeMethodKey(Signature signature) {
        return signature.toLongString();
    }

    /**
     * Dumps method usage in outputFile
     * <p/>
     * Thread-safe.
     */
    public static void dumpCodeUsage() {
        detectUnusedMethods();

        try {
            long startedAt = System.currentTimeMillis();

            File tmpFile = File.createTempFile("duck", ".tmp", config.getDataFile().getAbsoluteFile().getParentFile());

            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            int num = dumpCount.incrementAndGet();
            Date dumpedAt = new Date();

            out.printf("# Duck usage results #%d for '%s' at %s:%n", num, config.getAppName(), dumpedAt);

            // Only iterate over trackedMethods once, it could be updated anytime
            List<Usage> usages = new ArrayList<Usage>(trackedMethods.values());

            out.println("# lastUsedMillis signature");

            out.println("# Unused methods:");
            int unused = 0;
            for (Usage usage : usages) {
                if (usage.usedAtMillis == 0L) {
                    out.println(usage);
                    unused += 1;
                }
            }

            out.println("# Used methods:");
            int used = 0;
            for (Usage usage : usages) {
                if (usage.usedAtMillis > 0L) {
                    out.println(usage);
                    used += 1;
                }
            }
            long elapsed = System.currentTimeMillis() - startedAt;
            out.printf("# Dump #%d for '%s' at %s took %d ms, unused methods: %d, used methods: %d%n", num, config.getAppName(), dumpedAt,
                       elapsed, unused, used);

            out.flush();
            out.close();

            if (!tmpFile.renameTo(config.getDataFile())) {
                System.err.printf("%s: Could not rename %s to %sms%n", MY_NAME, tmpFile.getAbsolutePath(),
                                  config.getDataFile().getAbsolutePath());
            }
            System.err.printf("%s: Dumping usage data #%d to %s%n", MY_NAME, num, config.getDataFile().toURI());

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private static void detectUnusedMethods() {

        if (classpathScanned.getAndSet(true)) {
            // classpath was already scanned
            return;
        }

        long startedAt = System.currentTimeMillis();

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(config.getPackagePrefix()), new SubTypesScanner(false));

        int count = 0;
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isSynthetic()) {
                    // Use AspectJ for creating the same signature as AbstractDuckAspect...
                    MethodSignature signature = new Factory(null, clazz)
                            .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                                           null, method.getExceptionTypes(), method.getReturnType());

                    Usage usage = new Usage(makeMethodKey(signature), 0L);
                    trackedMethods.putIfAbsent(usage.signature, usage);
                    count += 1;
                }
            }
        }

        System.err.printf("%s: Classpath with package prefix '%s' scanned in %d ms, found %d methods.%n",
                          MY_NAME, config.getPackagePrefix(), System.currentTimeMillis() - startedAt, count);
    }

}
