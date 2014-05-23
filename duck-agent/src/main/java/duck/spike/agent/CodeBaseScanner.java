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

/**
 * @author Olle Hallin
 */
@Slf4j
public class CodeBaseScanner {

    public static class Result {
        public final Set<String> signatures = new TreeSet<>();
        public final Map<String, String> overriddenSignatures = new HashMap<>();
    }

    @SneakyThrows(MalformedURLException.class)
    Result getPublicMethodSignatures(Configuration config) {
        File codeBase = new File(config.getCodeBaseUri());
        checkState(codeBase.exists(), "Code base at " + codeBase + " does not exist");

        long startedAt = System.currentTimeMillis();
        log.info("Scanning code base at {}", config.getCodeBaseUri());

        URLClassLoader appClassLoader = new URLClassLoader(getUrlsForCodeBase(codeBase), System.class.getClassLoader());
        Reflections reflections = new Reflections(config.getPackagePrefix(), appClassLoader, new SubTypesScanner(false));

        Result result = new Result();

        for (Class<?> clazz : reflections.getSubTypesOf(Object.class)) {
            findPublicMethods(clazz, result.signatures, result.overriddenSignatures, config.getPackagePrefix());
        }

        checkState(!result.signatures.isEmpty(),
                   "Code base at " + codeBase + " does not contain any classes with package prefix " + config.getPackagePrefix());

        log.debug("Code base at {} with package prefix '{}' scanned in {} ms, found {} public methods.",
                  config.getCodeBaseUri(), config.getPackagePrefix(), System.currentTimeMillis() - startedAt, result.signatures.size());
        return result;
    }

    void findPublicMethods(Class<?> clazz, Set<String> result, Map<String, String> overriddenMethods, String packagePrefix) {
        String prefix = " " + packagePrefix;
        for (Method method : clazz.getMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                String declaringSignature =
                        AspectjUtils.makeMethodKey(AspectjUtils.makeMethodSignature(method.getDeclaringClass(), method));
                String thisSignature = AspectjUtils.makeMethodKey(AspectjUtils.makeMethodSignature(clazz, method));

                if (!thisSignature.equals(declaringSignature) && declaringSignature.contains(prefix)) {
                    // Some AOP frameworks (e.g., Guice) create subclasses on the fly containing overridden methods from the base class.
                    // We need to map those back to the original signature in the base class, or else the base method will look unused.
                    overriddenMethods.put(thisSignature, declaringSignature);
                }

                if (result.add(declaringSignature)) {
                    log.trace("  Found {}", declaringSignature);
                }
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
