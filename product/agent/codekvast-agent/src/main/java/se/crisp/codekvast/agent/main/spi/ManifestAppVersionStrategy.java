package se.crisp.codekvast.agent.main.spi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * A strategy for picking the app version from a jar manifest.
 * <p/>
 * It handles the cases {@code manifest some.jar} and {@code manifest some.jar My-Custom-Manifest-Attribute} where some.jar can be either an
 * URI or a file path.
 * <p/>
 * The two-arg version uses {@code Implementation-Version} as manifest attribute.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Slf4j
@Component
public class ManifestAppVersionStrategy implements AppVersionStrategy {

    private static final String DEFAULT_MANIFEST_ATTRIBUTE = "Implementation-Version";

    @Override
    public boolean canHandle(String[] args) {
        return args != null && (args.length == 2 || args.length == 3) && recognizes(args[0]);
    }

    private boolean recognizes(String name) {
        return name.equalsIgnoreCase("manifest");
    }

    @Override
    public String resolveAppVersion(String[] args) {
        String jarUri = args[1];
        String manifestAttribute = args.length > 2 ? args[2] : DEFAULT_MANIFEST_ATTRIBUTE;
        try {
            File file = getJarFile(jarUri);
            JarFile jarFile = new JarFile(file);
            Attributes attributes = jarFile.getManifest().getMainAttributes();
            String resolvedVersion = attributes.getValue(manifestAttribute);
            log.debug("Resolved version='{}'", resolvedVersion);
            return resolvedVersion;
        } catch (Exception e) {
            log.warn("Cannot open " + jarUri, e);
        }
        log.warn("Could not resolve {} -> {}", jarUri, manifestAttribute);
        return "<unknown>";
    }

    private File getJarFile(String jarUri) throws IOException, URISyntaxException {
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
        File result = url == null ? null : new File(url.toURI());
        if (result != null && !result.canRead()) {
            throw new IOException("Cannot read " + result);
        }
        return result;
    }
}
