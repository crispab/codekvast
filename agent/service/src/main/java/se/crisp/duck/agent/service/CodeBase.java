package se.crisp.duck.agent.service;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@ToString(of = "file", includeFieldNames = false)
@EqualsAndHashCode
@Slf4j
public class CodeBase {
    private final File file;
    private List<URL> urls = null;
    private long lastModifiedAtMillis;
    private long totalSize;

    public CodeBase(String codeBasePath) {
        this.file = new File(codeBasePath);
        checkArgument(file.exists(), "Code base at " + file + " does not exist");
        initUrls();
    }

    public URL[] getUrls() {
        return urls.toArray(new URL[urls.size()]);
    }

    @SneakyThrows(MalformedURLException.class)
    private void initUrls() {

        urls = new ArrayList<>();
        lastModifiedAtMillis = 0L;
        totalSize = 0L;

        if (file.isDirectory()) {
            scanExplodedDirectory();
        } else if (file.getName().endsWith(".jar")) {
            updateFingerprint(file);
            addUrl(file);
        } else if (file.getName().endsWith(".war")) {
            updateFingerprint(file);
            throw new UnsupportedOperationException("Scanning WAR not yet supported");
        } else if (file.getName().endsWith(".ear")) {
            updateFingerprint(file);
            throw new UnsupportedOperationException("Scanning EAR not yet supported");
        }
    }

    private void addUrl(File file) throws MalformedURLException {
        urls.add(file.toURI().toURL());
    }

    private void updateFingerprint(File file) {
        lastModifiedAtMillis = Math.max(lastModifiedAtMillis, file.lastModified());
        totalSize += file.length();
        log.debug("Added {}, totalSize is now {}, lastModified is now {}", file, totalSize, lastModifiedAtMillis);
    }

    private void scanExplodedDirectory() throws MalformedURLException {
        log.debug("Scanning directory {}...", file);

        addUrl(file);

        // Look for jars in that directory
        File[] jarFiles = file.listFiles(new JarNameFilter());

        for (File jarFile : jarFiles) {
            if (jarFile.canRead()) {
                updateFingerprint(jarFile);
                addUrl(jarFile);
            } else {
                log.warn("Ignoring {} since it cannot be read", jarFile);
            }
        }
    }

    private static class JarNameFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            boolean isJar = file.isFile() && file.getName().endsWith(".jar");
            if (!isJar) {
                log.debug("  Ignoring {}, not a jar file", file);
            }
            return isJar;
        }
    }

}
