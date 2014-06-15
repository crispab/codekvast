package se.crisp.duck.agent.main;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import se.crisp.duck.agent.util.AgentConfig;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@ToString(of = "codeBaseFile", includeFieldNames = false)
@Slf4j
class CodeBase {

    private static final Pattern[] ENHANCE_BY_GUICE_PATTERNS = {
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.getIndex\\(java\\.lang\\.Class\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.newInstance\\(int, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.invoke\\(int, java\\.lang\\.Object, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\(com\\.google\\.inject\\.internal\\.cglib.*\\)$"),
    };

    private final File codeBaseFile;
    @Getter
    private final AgentConfig config;
    @Getter
    final Set<String> signatures = new HashSet<>();
    final Map<String, String> overriddenSignatures = new HashMap<>();

    private CodeBaseFingerprint fingerprint;
    private List<URL> urls;
    private boolean needsExploding = false;

    CodeBase(AgentConfig config) {
        this.config = config;
        this.codeBaseFile = new File(config.getCodeBaseUri());
    }

    URL[] getUrls() {
        getFingerprint();
        if (needsExploding) {
            throw new UnsupportedOperationException("Exploding WAR or EAR not yet implemented");
        }
        return urls.toArray(new URL[urls.size()]);
    }

    CodeBaseFingerprint getFingerprint() {
        if (fingerprint == null) {
            fingerprint = initUrls();
        }
        return fingerprint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CodeBase that = (CodeBase) o;

        return this.getFingerprint().equals(that.getFingerprint());
    }

    @Override
    public int hashCode() {
        return getFingerprint().hashCode();
    }

    private CodeBaseFingerprint initUrls() {
        long startedAt = System.currentTimeMillis();

        urls = new ArrayList<>();
        CodeBaseFingerprint.Builder builder = CodeBaseFingerprint.builder();

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
        CodeBaseFingerprint result = builder.build();

        log.debug("Made fingerprint of code base at {} in {} ms, fingerprint={}", codeBaseFile, System.currentTimeMillis() - startedAt,
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

    void scanSignatures(CodeBaseScanner codeBaseScanner) {
        long startedAt = System.currentTimeMillis();
        log.info("Scanning code base {}", this);

        codeBaseScanner.getPublicMethodSignatures(this);

        if (signatures.isEmpty()) {
            log.warn("Code base at {} does not contain any classes with package prefix '{}'", codeBaseFile, config.getPackagePrefix());
        } else {
            writeSignaturesTo(config.getSignatureFile());

            log.debug("Code base {} with package prefix '{}' scanned in {} ms, found {} public methods.",
                      codeBaseFile, config.getPackagePrefix(), System.currentTimeMillis() - startedAt, signatures.size());
        }
    }

    void addSignature(String thisSignature, String declaringSignature) {
        String thisNormalizedSignature = normalizeSignature(thisSignature);
        String declaringNormalizedSignature = normalizeSignature(declaringSignature);

        if (declaringNormalizedSignature != null) {
            if (!declaringNormalizedSignature.equals(thisNormalizedSignature) && thisNormalizedSignature != null) {
                log.trace("  Adding {} -> {} to overridden signatures", thisNormalizedSignature, declaringNormalizedSignature);
                overriddenSignatures.put(thisNormalizedSignature, declaringNormalizedSignature);
            }

            if (signatures.add(declaringNormalizedSignature)) {
                log.trace("  Found {}", declaringNormalizedSignature);
            }
        }
    }

    private void writeSignaturesTo(File file) {
        PrintWriter out = null;
        try {
            File directory = file.getAbsoluteFile().getParentFile();
            directory.mkdirs();

            File tmpFile = File.createTempFile("duck", ".tmp", directory);
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

    boolean hasSignature(String signature) {
        return signatures.contains(signature);
    }

    String getBaseSignature(String signature) {
        return overriddenSignatures.get(signature);
    }

    String normalizeSignature(String signature) {
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

    @VisibleForTesting
    void readScannerResult(File file) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            boolean signaturePhase = true;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    // Ignore comments and empty lines
                } else if (line.contains("-----------")) {
                    signaturePhase = false;
                } else if (signaturePhase) {
                    signatures.add(normalizeSignature(line));
                } else {
                    String parts[] = line.split("->");
                    overriddenSignatures.put(normalizeSignature(parts[0]), normalizeSignature(parts[1]));
                }
            }
        }

    }
}
