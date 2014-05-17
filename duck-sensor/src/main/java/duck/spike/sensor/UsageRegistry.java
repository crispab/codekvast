package duck.spike.sensor;

import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import duck.spike.util.Usage;
import org.aspectj.lang.Signature;

import java.io.*;
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
            System.err.printf("%s: Found %s, will continue from that%n", UsageRegistry.MY_NAME, config.getDataFile().getAbsolutePath());
            Usage.readUsagesFromFile(trackedMethods, config.getDataFile());
        }
    }

    /**
     * This method is invoked by {@link duck.spike.sensor.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain type and method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public static void registerMethodExecution(Signature signature) {
        Usage usage = new Usage(AspectjUtils.makeMethodKey(signature), System.currentTimeMillis());
        trackedMethods.put(usage.getSignature(), usage);
    }

    /**
     * Dumps method usage in outputFile
     * <p/>
     * Thread-safe.
     */
    public static void dumpCodeUsage() {
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
            int count = 0;
            for (Usage usage : usages) {
                    out.println(usage);
                count += 1;
            }
            long elapsed = System.currentTimeMillis() - startedAt;
            out.printf("# Dump #%d for '%s' at %s took %d ms, number of methods: %d%n", num, config.getAppName(), dumpedAt,
                       elapsed, count);

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

}
