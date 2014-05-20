package duck.spike.sensor;

import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import duck.spike.util.SensorRun;
import duck.spike.util.Usage;
import org.aspectj.lang.Signature;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olle Hallin
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "Singleton", "FeatureEnvy"})
public class UsageRegistry {

    private static final String MY_NAME = UsageRegistry.class.getSimpleName();

    public static UsageRegistry instance;

    private final Configuration config;
    private final SensorRun sensorRun;

    private final ConcurrentMap<String, Usage> usages = new ConcurrentHashMap<String, Usage>();
    private int dumpCount = 0;

    private UsageRegistry(Configuration config, SensorRun sensorRun) {
        this.config = config;
        this.sensorRun = sensorRun;

        File parent = config.getDataFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(Configuration config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   SensorRun.builder()
                                                            .hostName(getHostName())
                                                            .uuid(UUID.randomUUID())
                                                            .startedAtMillis(System.currentTimeMillis())
                                                            .build()
        );
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.err.println(MY_NAME + ": Cannot get local hostname: " + e);
            return "-- unknown --";
        }
    }

    /**
     * This method is invoked by {@link duck.spike.sensor.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public void registerMethodExecution(Signature signature) {
        String sig = AspectjUtils.makeMethodKey(signature);
        usages.put(sig, new Usage(sig, System.currentTimeMillis()));
    }

    /**
     * Dumps method usage to a file on disk.
     * <p/>
     * Thread-safe.
     */
    public synchronized void dumpDataToDisk() {
        instance.dumpSensorRun();
        instance.dumpUsageData();
    }

    private void dumpSensorRun() {
        File file = config.getSensorFile();

        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            sensorRun.saveTo(tmpFile);
            renameFile(tmpFile, file);
        } catch (IOException e) {
            System.err.println(MY_NAME + ": Cannot save " + file + ": " + e);
        }
    }

    private void dumpUsageData() {
        dumpCount += 1;
        long startedAt = System.currentTimeMillis();
        File file = config.getDataFile();

        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());

            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            System.err.printf(Locale.ENGLISH, "%s: Dumping usage data #%d to %s%n", MY_NAME, dumpCount, file.toURI());

            Date dumpedAt = new Date();

            out.printf(Locale.ENGLISH, "# Duck usage results #%d for '%s' at %s%n", dumpCount, config.getAppName(), dumpedAt);
            out.printf(Locale.ENGLISH, "# lastUsedMillis:signature%n");

            // Only iterate over trackedMethods once
            Iterable<Usage> usages = new ArrayList<Usage>(this.usages.values());

            int count = 0;
            for (Usage usage : usages) {
                out.println(usage);
                count += 1;
            }
            long elapsed = System.currentTimeMillis() - startedAt;
            out.printf(Locale.ENGLISH, "# Dump #%d for '%s' at %s took %d ms, number of methods: %d%n", dumpCount, config.getAppName(),
                       dumpedAt,
                       elapsed, count);

            out.flush();
            out.close();

            renameFile(tmpFile, file);
        } catch (IOException e) {
            System.err.println(MY_NAME + ": Cannot dump usage data to " + file + ": " + e);
        }
    }

    private void renameFile(File from, File to) {
        if (!from.renameTo(to)) {
            System.err.printf(Locale.ENGLISH, "%s: Could not rename %s to %s%n", MY_NAME, from.getAbsolutePath(),
                              to.getAbsolutePath());
            from.delete();
        }
    }

}
