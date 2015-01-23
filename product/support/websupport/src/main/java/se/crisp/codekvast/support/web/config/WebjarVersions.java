package se.crisp.codekvast.support.web.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Slf4j
public class WebjarVersions {

    private final Map versions = new HashMap<>();

    public WebjarVersions() {
        scanWebjars();
    }

    private void scanWebjars() {
        ClassLoader cl = WebjarVersions.class.getClassLoader();

        if (!(cl instanceof URLClassLoader)) {
            throw new UnsupportedOperationException("Don't know how to scan classpath from " + cl.getClass().getName());
        }

        log.debug("Scanning classpath for webjars...");

        URLClassLoader ucl = (URLClassLoader) cl;
        Pattern pattern = Pattern.compile(".*webjars/(.*?)/(.*?)/.*");

        // For each URL in classpath
        for (URL url : ucl.getURLs()) {
            try {
                analyzeJar(url, pattern);
            } catch (IOException e) {
                log.warn("Cannot analyze " + url, e);
            }
        }
        log.debug("Found {} webjars", versions.size());
    }

    private void analyzeJar(URL jarUrl, Pattern pattern) throws IOException {
        // Look for entries that match the pattern...
        JarInputStream inputStream = new JarInputStream(jarUrl.openStream());

        JarEntry jarEntry = inputStream.getNextJarEntry();
        while (jarEntry != null) {
            log.trace("Considering {}", jarEntry);

            Matcher matcher = pattern.matcher(jarEntry.getName());
            if (matcher.matches()) {
                String key = matcher.group(1).replaceAll("[_-]", "").toLowerCase() + "Version";
                String value = matcher.group(2);
                versions.put(key, value);
                log.debug("{} is a webjar, adding {}={} to map", basename(jarUrl.getPath()), key, value);
                return;
            }
            jarEntry = inputStream.getNextJarEntry();
        }
    }

    private String basename(String path) {
        int slash = path.replace('\\', '/').lastIndexOf('/');
        return path.substring(slash + 1);
    }

    public Map getVersions() {
        return versions;
    }
}
