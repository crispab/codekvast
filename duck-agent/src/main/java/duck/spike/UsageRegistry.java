package duck.spike;

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

/**
 * @author Olle Hallin
 */
public class UsageRegistry {

    private static class Usage {
        private final Signature signature;
        private final long usedAtMillis;

        private Usage(Signature signature, long usedAtMillis) {
            this.signature = signature;
            this.usedAtMillis = usedAtMillis;
        }

        @Override
        public String toString() {
            return String.format("%14d %s", usedAtMillis, makeMethodKey(signature));
        }
    }

    private UsageRegistry() {
        // Utility class with only static methods
    }

    private static String packagePrefix;
    private static File outputFile;
    private static ConcurrentMap<String, Usage> trackedMethods = new ConcurrentHashMap<String, Usage>();
    private static AtomicBoolean classpathScanned = new AtomicBoolean(false);
    private static AtomicInteger dumpCount = new AtomicInteger();

    public static void initialize(String packagePrefix, File outputFile) {
        UsageRegistry.packagePrefix = packagePrefix;
        UsageRegistry.outputFile = outputFile;

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * This method is invoked by {@link duck.spike.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain type and method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public static void registerMethodExecution(Signature signature) {
        trackedMethods.put(makeMethodKey(signature), new Usage(signature, System.currentTimeMillis()));
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

            File tmpFile = File.createTempFile("duck", ".tmp", outputFile.getAbsoluteFile().getParentFile());

            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            int num = dumpCount.incrementAndGet();
            Date dumpedAt = new Date();

            out.printf("# Duck usage results #%d at %s:%n", num, dumpedAt);

            // Only iterate over trackedMethods once, it could be updated anytime
            List<Usage> usages = new ArrayList<Usage>(trackedMethods.values());

            out.println("# Unused methods:");
            int unused = 0;
            for (Usage usage : usages) {
                if (usage.usedAtMillis == 0L) {
                    out.println(usage);
                    unused += 1;
                }
            }

            out.println("# Used methods (first column is millis since epoch):");
            int used = 0;
            for (Usage usage : usages) {
                if (usage.usedAtMillis > 0L) {
                    out.println(usage);
                    used += 1;
                }
            }
            long elapsed = System.currentTimeMillis() - startedAt;
            out.printf("# Dump #%d at %s took %d ms, unused methods: %d, used methods: %d%n", num, dumpedAt, elapsed, unused, used);

            out.flush();
            out.close();

            if (!tmpFile.renameTo(outputFile)) {
                System.err.printf("%s: Could not rename %s to %sms%n", UsageRegistry.class.getName(), tmpFile.getAbsolutePath(),
                                  outputFile.getAbsolutePath());
            }

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

        Reflections reflections = new Reflections(ClasspathHelper.forPackage(packagePrefix), new SubTypesScanner(false));

        int count = 0;
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isSynthetic()) {
                    // Use AspectJ for creating the same signature as AbstractDuckAspect...
                    MethodSignature signature = new Factory(null, clazz)
                            .makeMethodSig(method.getModifiers(), method.getName(), method.getDeclaringClass(), method.getParameterTypes(),
                                           null, method.getExceptionTypes(), method.getReturnType());

                    trackedMethods.putIfAbsent(makeMethodKey(signature), new Usage(signature, 0L));
                    count += 1;
                }
            }
        }

        System.err.printf("%s: Classpath with package prefix '%s' scanned in %d ms, found %d methods.%n",
                          UsageRegistry.class.getName(), packagePrefix, System.currentTimeMillis() - startedAt, count);
    }

}
