package duck.spike.agent;

import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import duck.spike.util.Usage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.Signature;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Olle Hallin
 */
@Slf4j
@RequiredArgsConstructor
public class CodeBaseScanner {

    private final Configuration config;

    @SneakyThrows(MalformedURLException.class)
    Map<String, Usage> scanCodeBase() {

        File codeBase = new File(config.getCodeBaseUri());
        checkState(codeBase.exists(), "Code base at " + codeBase + " does not exist");

        long startedAt = System.currentTimeMillis();

        URLClassLoader appClassLoader = new URLClassLoader(getUrlsForCodeBase(codeBase), System.class.getClassLoader());

        Reflections reflections = new Reflections(config.getPackagePrefix(), appClassLoader, new SubTypesScanner(false));

        int count = 0;
        Map<String, Usage> result = new HashMap<>();
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                    Signature signature = AspectjUtils.makeMethodSignature(clazz, method);

                    Usage usage = new Usage(AspectjUtils.makeMethodKey(signature), 0L);
                    result.put(usage.getSignature(), usage);
                    count += 1;
                    log.debug("  Found {}", usage.getSignature());
                }
            }
        }

        checkState(count > 0,
                   "Code base at " + codeBase + " does not contain any classes with package prefix " + config.getPackagePrefix());

        log.debug("Code base at {} with package prefix '{}' scanned in {} ms, found {} public methods.",
                  config.getCodeBaseUri(), config.getPackagePrefix(), System.currentTimeMillis() - startedAt, count);
        return result;
    }

    URL[] getUrlsForCodeBase(File codeBase) throws MalformedURLException {
        List<URL> result = new ArrayList<>();

        // TODO: handle *.jar, *.war, *.ear, */ etc
        result.add(codeBase.toURI().toURL());

        log.debug("Scanning urls {}", result);
        return result.toArray(new URL[result.size()]);
    }

}
