package duck.spike.agent;


import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import duck.spike.util.Usage;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.aspectj.lang.reflect.MethodSignature;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Olle Hallin
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class Agent extends TimerTask {
    private static final String MY_NAME = Agent.class.getSimpleName();

    private final Configuration config;
    private final Timer timer = new Timer(MY_NAME, false);
    private final Map<String, Usage> usages = new HashMap<>();

    private void start() {
        long intervalMillis = config.getWarehouseUploadIntervalSeconds() * 1000L;
        scanCodeBase();
        timer.scheduleAtFixedRate(this, intervalMillis, intervalMillis);
        System.out.printf("%s started, config=%s%n", MY_NAME, config);
    }

    @Override
    public void run() {
        Usage.readUsagesFromFile(usages, config.getDataFile());
        System.out.printf("%s: Uploading usage data for %d methods from %s... in %s to %s%n", MY_NAME, usages.size(),
                          config.getPackagePrefix(), config.getAppName(), config.getWarehouseUri());
    }

    @SneakyThrows(MalformedURLException.class)
    private void scanCodeBase() {

        long startedAt = System.currentTimeMillis();

        URLClassLoader appClassLoader = new URLClassLoader(new URL[]{config.getCodeBaseUri().toURL()}, System.class.getClassLoader());

        Reflections reflections = new Reflections(
                config.getPackagePrefix(),
                appClassLoader,
                new SubTypesScanner(false));

        int count = 0;
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isSynthetic()) {
                    MethodSignature signature = AspectjUtils.getMethodSignature(clazz, method);


                    Usage usage = new Usage(AspectjUtils.makeMethodKey(signature), 0L);
                    usages.put(usage.getSignature(), usage);
                    count += 1;
                }
            }
        }

        System.err.printf("%s: Code base %s with package prefix '%s' scanned in %d ms, found %d methods.%n",
                          MY_NAME, config.getCodeBaseUri(), config.getPackagePrefix(), System.currentTimeMillis() - startedAt, count);
    }

    public static void main(String[] args) {
        Configuration config = Configuration.parseConfigFile("/path/to/duck.properties");
        Agent agent = new Agent(config);
        agent.start();
    }

}
