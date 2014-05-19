package duck.spike.agent;


import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import duck.spike.util.SensorRun;
import duck.spike.util.Usage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.Signature;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Olle Hallin
 */
@RequiredArgsConstructor
public class Agent extends TimerTask {
    private final Configuration config;
    private final Map<String, Usage> usages = new HashMap<>();

    private Timer timer;
    private long dataFileModifiedAtMillis;
    private UUID lastSeenSensorUUID;

    private void start() {
        timer = new Timer(getClass().getSimpleName(), false);
        long intervalMillis = config.getWarehouseUploadIntervalSeconds() * 1000L;
        timer.scheduleAtFixedRate(this, 0L, intervalMillis);
        System.out.printf("Started with %s%n", config);
    }

    @Override
    public void run() {
        SensorRun sensorRun = scanClasspathIfNewSensorRun();
        if (sensorRun == null) {
            System.out.printf("%s not found%n", config.getSensorFile());
            return;
        }

        long modifiedAt = config.getDataFile().lastModified();
        if (modifiedAt != dataFileModifiedAtMillis) {
            Usage.readUsagesFromFile(usages, config.getDataFile());
            int unused = 0;
            int used = 0;

            for (Usage usage : usages.values()) {
                if (usage.getUsedAtMillis() == 0L) {
                    unused += 1;
                }
                if (usage.getUsedAtMillis() != 0L) {
                    used += 1;
                }
            }

            System.out.printf("Posting usage data for %d unused and %d used methods in %s to %s%n", unused, used,
                              config.getAppName(), config.getWarehouseUri());

            // TODO: post sensorRun and usages to data warehouse

            dataFileModifiedAtMillis = modifiedAt;
        }
    }

    private SensorRun scanClasspathIfNewSensorRun() {
        SensorRun sensorRun;
        try {
            sensorRun = SensorRun.readFrom(config.getSensorFile());
        } catch (IOException e) {
            return null;
        }

        if (lastSeenSensorUUID == null || !lastSeenSensorUUID.equals(sensorRun.getUuid())) {
            System.out.printf("Scanning code base at %s%n", config.getCodeBaseUri());
            scanCodeBase();
            lastSeenSensorUUID = sensorRun.getUuid();
        }
        return sensorRun;
    }

    @SneakyThrows(MalformedURLException.class)
    private void scanCodeBase() {

        File codeBase = new File(config.getCodeBaseUri());
        checkState(codeBase.exists(), "Code base at " + codeBase + " does not exist");

        long startedAt = System.currentTimeMillis();

        URLClassLoader appClassLoader = new URLClassLoader(new URL[]{codeBase.toURI().toURL()}, System.class.getClassLoader());

        Reflections reflections = new Reflections(config.getPackagePrefix(), appClassLoader, new SubTypesScanner(false));

        int count = 0;
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                    Signature signature = AspectjUtils.makeMethodSignature(clazz, method);

                    Usage usage = new Usage(AspectjUtils.makeMethodKey(signature), 0L);
                    usages.put(usage.getSignature(), usage);
                    count += 1;
                    System.out.printf("  Found %s%n", usage.getSignature());
                }
            }
        }

        checkState(count > 0,
                   "Code base at " + codeBase + " does not contain any classes with package prefix " + config.getPackagePrefix());

        System.out.printf("Code base at %s with package prefix '%s' scanned in %d ms, found %d public methods.%n",
                          config.getCodeBaseUri(), config.getPackagePrefix(), System.currentTimeMillis() - startedAt, count);
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.println("Usage: agent <path/to/duck.properties>");
            System.exit(1);
        }

        Configuration config = null;

        try {
            config = Configuration.parseConfigFile(args[0]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        new Agent(config).start();
    }

}
