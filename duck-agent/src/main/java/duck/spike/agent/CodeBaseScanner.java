package duck.spike.agent;

import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

/**
 * @author Olle Hallin
 */
@Slf4j
public class CodeBaseScanner {

    @SneakyThrows(MalformedURLException.class)
    Set<String> getPublicMethodSignatures(Configuration config) {
        File codeBase = new File(config.getCodeBaseUri());
        checkState(codeBase.exists(), "Code base at " + codeBase + " does not exist");

        long startedAt = System.currentTimeMillis();
        log.info("Scanning code base at {}", config.getCodeBaseUri());

        URLClassLoader appClassLoader = new URLClassLoader(getUrlsForCodeBase(codeBase), System.class.getClassLoader());
        Reflections reflections = new Reflections(config.getPackagePrefix(), appClassLoader, new SubTypesScanner(false));

        Set<String> debugNames = new HashSet<>(asList("EAOBase", "CalculationAlgorithm", "LayerRate", "FlowDirection"));

        Set<String> result = new TreeSet<>();
        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            if (debugNames.contains(clazz.getSimpleName())) {
                log.debug("Analyzing " + clazz);
            }
            for (Method method : clazz.getMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    addSignature(clazz, method, result, config.getPackagePrefix());
                }
            }
        }

        checkState(!result.isEmpty(),
                   "Code base at " + codeBase + " does not contain any classes with package prefix " + config.getPackagePrefix());

        log.debug("Code base at {} with package prefix '{}' scanned in {} ms, found {} public methods.",
                  config.getCodeBaseUri(), config.getPackagePrefix(), System.currentTimeMillis() - startedAt, result.size());
        return result;
    }

    private void addSignature(Class<?> clazz, Method method, Set<String> result, String packagePrefix) {
        if (method.getDeclaringClass().getPackage().getName().startsWith(packagePrefix)) {
            String signature = AspectjUtils.makeMethodKey(AspectjUtils.makeMethodSignature(method));
            if (result.add(signature)) {
                log.trace("  Found {}", signature);
            }
        }
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
                boolean isJar = file.isFile() && file.getName().endsWith(".jar");
                if (!isJar) {
                    log.debug("  Ignoring {}, not a jar file", file);
                }
                return isJar;
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
