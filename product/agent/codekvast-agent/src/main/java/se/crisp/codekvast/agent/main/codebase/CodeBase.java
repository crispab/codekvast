package se.crisp.codekvast.agent.main.codebase;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import se.crisp.codekvast.agent.config.CollectorConfig;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Handles a code base, i.e., the set of public methods of an application.
 *
 * @author olle.hallin@crisp.se
 */
@ToString(of = "codeBaseFiles", includeFieldNames = false)
@EqualsAndHashCode(of = "fingerprint")
@Slf4j
public class CodeBase {

    private static final Pattern[] ENHANCE_BY_GUICE_PATTERNS = {
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.getIndex\\(java\\.lang\\.Class\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.newInstance\\(int, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.invoke\\(int, java\\.lang\\.Object, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\(com\\.google\\.inject\\.internal\\.cglib.*\\)$"),
    };

    private final List<File> codeBaseFiles;

    @Getter
    private final CollectorConfig config;

    @Getter
    private final Set<String> signatures = new TreeSet<String>();

    @Getter
    private final Map<String, String> overriddenSignatures = new HashMap<String, String>();

    private final CodeBaseFingerprint fingerprint;

    private List<URL> urls;
    private boolean needsExploding = false;

    public CodeBase(CollectorConfig config) {
        this.config = config;
        this.codeBaseFiles = config.getCodeBaseFiles();
        this.fingerprint = initUrls();
    }

    URL[] getUrls() {
        if (needsExploding) {
            throw new UnsupportedOperationException("Exploding WAR or EAR not yet implemented");
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private CodeBaseFingerprint initUrls() {
        long startedAt = System.currentTimeMillis();

        urls = new ArrayList<URL>();
        CodeBaseFingerprint.Builder builder = CodeBaseFingerprint.builder();
        for (File codeBaseFile : codeBaseFiles) {
            if (codeBaseFile.isDirectory()) {
                addUrl(codeBaseFile);
                traverse(builder, codeBaseFile.listFiles());
            } else if (codeBaseFile.getName().endsWith(".jar")) {
                builder.record(codeBaseFile);
                addUrl(codeBaseFile);
            } else if (codeBaseFile.getName().endsWith(".war")) {
                builder.record(codeBaseFile);
                needsExploding = true;
            } else if (codeBaseFile.getName().endsWith(".ear")) {
                builder.record(codeBaseFile);
                needsExploding = true;
            }
        }

        CodeBaseFingerprint result = builder.build();

        log.debug("Made fingerprint of code bases at {} in {} ms, fingerprint={}", codeBaseFiles, System.currentTimeMillis() - startedAt,
                  result);
        return result;
    }

    @SneakyThrows(MalformedURLException.class)
    private void addUrl(File file) {
        log.trace("Adding URL {}", file);
        urls.add(file.toURI().toURL());
    }

    private void traverse(CodeBaseFingerprint.Builder builder, File[] files) {
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    builder.record(file);
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    builder.record(file);
                    addUrl(file);
                } else if (file.isDirectory()) {
                    traverse(builder, file.listFiles());
                }
            }
        }
    }

    void addSignature(String thisSignature, String declaringSignature) {
        String thisNormalizedSignature = normalizeSignature(thisSignature);
        String declaringNormalizedSignature = normalizeSignature(declaringSignature);

        if (declaringNormalizedSignature != null) {
            if (!declaringNormalizedSignature.equals(thisNormalizedSignature) && thisNormalizedSignature != null) {
                log.trace("  Adding {} -> {} to overridden signatures", thisNormalizedSignature, declaringNormalizedSignature);
                overriddenSignatures.put(thisNormalizedSignature, declaringNormalizedSignature);
            } else if (declaringNormalizedSignature != null && signatures.add(declaringNormalizedSignature)) {
                log.trace("  Found {}", declaringNormalizedSignature);
            }
        }
    }

    void writeSignaturesToDisk() {
        File file = config.getSignatureFile(config.getAppName());
        PrintWriter out = null;
        try {
            File directory = file.getAbsoluteFile().getParentFile();
            directory.mkdirs();

            File tmpFile = File.createTempFile("codekvast", ".tmp", directory);
            out = new PrintWriter(tmpFile, "UTF-8");

            out.println("# Signatures:");
            for (String signature : signatures) {
                out.println(signature);
            }

            out.println();
            out.println("------------------------------------------------------------------------------------------------");
            out.println("# Overridden signatures:");
            out.println("# child() -> base()");
            for (Map.Entry<String, String> entry : overriddenSignatures.entrySet()) {
                out.printf("%s -> %s%n", entry.getKey(), entry.getValue());
            }

            if (!tmpFile.renameTo(file)) {
                log.error("Cannot rename {} to {}", tmpFile.getAbsolutePath(), file.getAbsolutePath());
                tmpFile.delete();
            }
        } catch (IOException e) {
            log.error("Cannot create " + file, e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public boolean hasSignature(String signature) {
        return signatures.contains(signature);
    }

    public String getBaseSignature(String signature) {
        return overriddenSignatures.get(signature);
    }

    public String normalizeSignature(String signature) {
        if (signature == null) {
            return null;
        }
        for (Pattern pattern : ENHANCE_BY_GUICE_PATTERNS) {
            if (pattern.matcher(signature).matches()) {
                return null;
            }
        }
        return signature.replaceAll(" final ", " ").replaceAll("\\.\\.EnhancerByGuice\\.\\..*[0-9a-f]\\.([\\w]+\\()", ".$1").trim();
    }

    public boolean isEmpty() {
        return signatures.isEmpty();
    }

    public int size() {
        return signatures.size();
    }
}
