package se.crisp.codekvast.agent.main.appversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * A strategy for picking the app version from a jar manifest.
 *
 * It handles the cases {@code manifest some.jar} and {@code manifest some.jar My-Custom-Manifest-Attribute} where some.jar can be either an
 * URI or a file path.
 *
 * The two-arg version uses {@code Implementation-Version} as manifest attribute.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
@Component
public class ManifestAppVersionStrategy extends AbstractAppVersionStrategy {

    private static final String DEFAULT_MANIFEST_ATTRIBUTE = "Implementation-Version";

    public ManifestAppVersionStrategy() {
        super("manifest", "search");
    }

    @Override
    public boolean canHandle(String[] args) {
        return args != null && (args.length == 2 || args.length == 3) && recognizes(args[0]);
    }

    @Override
    public String resolveAppVersion(Collection<File> codeBases, String[] args) {
        String jarUri = args[1];
        String manifestAttribute = args.length > 2 ? args[2] : DEFAULT_MANIFEST_ATTRIBUTE;
        for (File codeBaseFile : codeBases) {
            try {
                File file = getJarFile(codeBaseFile, jarUri);
                JarFile jarFile = new JarFile(file);
                Attributes attributes = jarFile.getManifest().getMainAttributes();
                String resolvedVersion = attributes.getValue(manifestAttribute);
                if (resolvedVersion != null) {
                    log.info("{}!/META-INF/MANIFEST.MF:{}={}", jarUri, manifestAttribute, resolvedVersion);
                    return resolvedVersion;
                }
                if (!manifestAttribute.equalsIgnoreCase(DEFAULT_MANIFEST_ATTRIBUTE)) {
                    resolvedVersion = attributes.getValue(DEFAULT_MANIFEST_ATTRIBUTE);
                }
                if (resolvedVersion != null) {
                    log.info("{}!/META-INF/MANIFEST.MF:{}={}", jarUri, DEFAULT_MANIFEST_ATTRIBUTE, resolvedVersion);
                    return resolvedVersion;
                }
            } catch (Exception e) {
                log.error("Cannot open " + jarUri + ": " + e);
            }
        }
        log.error("Cannot resolve {}!/META-INF/MANIFEST.MF:{}", jarUri, manifestAttribute);
        return UNKNOWN_VERSION;
    }

    private File getJarFile(File codeBaseFile, String jarUri) throws IOException, URISyntaxException {
        URL url = null;
        // try to parse it as a URL...
        try {
            url = new URL(jarUri);
        } catch (MalformedURLException ignore) {
        }

        if (url == null) {
            // Try to treat it as a file...
            File file = new File(jarUri);
            if (file.isFile() && file.canRead() && file.getName().endsWith(".jar")) {
                url = file.toURI().toURL();
            }
        }
        if (url == null) {
            // Search for it in codeBaseFile. Treat it as a regular expression for the basename
            url = search(codeBaseFile, jarUri);
        }

        File result = url == null ? null : new File(url.toURI());
        if (result == null || !result.canRead()) {
            throw new IOException("Cannot read " + jarUri);
        }
        return result;
    }

    private URL search(File dir, String regex) throws MalformedURLException {
        if (!dir.isDirectory()) {
            log.warn("{} is not a directory", dir);
            return null;
        }

        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().matches(regex)) {
                    log.debug("Found {}", file);
                    return new URL(file.toURI().toString());
                }
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    URL url = search(file, regex);
                    if (url != null) {
                        return url;
                    }
                }
            }
        }
        return null;
    }

}
