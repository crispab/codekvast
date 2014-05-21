package duck.spike.agent;

import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import duck.spike.util.Usage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.Signature;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Olle Hallin
 */
@Slf4j
public class CodeBaseScanner {

    @SneakyThrows(MalformedURLException.class)
    Map<String, Usage> scanCodeBase(Configuration config) {

        File codeBase = new File(config.getCodeBaseUri());
        checkState(codeBase.exists(), "Code base at " + codeBase + " does not exist");

        long startedAt = System.currentTimeMillis();

        URLClassLoader appClassLoader = new URLClassLoader(getUrlsForCodeBase(codeBase), System.class.getClassLoader());

        Reflections reflections = new Reflections(config.getPackagePrefix(), appClassLoader, new SubTypesScanner(false));

        int count = 0;
        Map<String, Usage> result = new TreeMap<>();
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                    Signature signature = AspectjUtils.makeMethodSignature(clazz, method);

                    Usage usage = new Usage(AspectjUtils.makeMethodKey(signature), 0L);
                    result.put(usage.getSignature(), usage);
                    count += 1;
                    log.trace("  Found {}", usage.getSignature());
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
        checkArgument(codeBase.exists(), "Code base at " + codeBase + " does not exist");

        List<URL> result = new ArrayList<>();
        if (codeBase.isDirectory()) {
            scanExplodedDirectory(codeBase, result);
        } else if (codeBase.getName().endsWith(".war")) {
            throw new UnsupportedOperationException("Scanning WAR not yet supported");
        } else if (codeBase.getName().endsWith(".ear")) {
            throw new UnsupportedOperationException("Scanning EAR not yet supported");
        } else if (codeBase.getName().endsWith(".jar")) {
            result.add(codeBase.toURI().toURL());
        }

        return result.toArray(new URL[result.size()]);
    }

    private void scanExplodedDirectory(File directory, List<URL> result) throws MalformedURLException {
        log.debug("Scanning directory {}...", directory);

        result.add(directory.toURI().toURL());

        // Look for jars in that directory
        File[] jarFiles = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                boolean result = file.isFile() && file.getName().endsWith(".jar");
                if (!result) {
                    log.debug("  Ignoring {}, not a jar...", file);
                }
                return result;
            }
        });

        for (File jarFile : jarFiles) {
            if (jarFile.canRead()) {
                log.debug("  Found {}", jarFile);
                result.add(jarFile.toURI().toURL());
            } else {
                log.warn("Ignoring {} since it cannot be read", jarFile);
            }
        }
    }

}
