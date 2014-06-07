package se.crisp.duck.agent.service;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import se.crisp.duck.agent.util.Configuration;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@ToString(of = "codeBaseFile", includeFieldNames = false)
@Slf4j
public class CodeBase {

    private static final Pattern[] ENHANCE_BY_GUICE_PATTERNS = {
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.getIndex\\(java\\.lang\\.Class\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.newInstance\\(int, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.invoke\\(int, java\\.lang\\.Object, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\(com\\.google\\.inject\\.internal\\.cglib.*\\)$"),
    };

    private final File codeBaseFile;
    @Getter
    private final Configuration config;
    final Set<String> signatures = new HashSet<>();
    final Map<String, String> overriddenSignatures = new HashMap<>();

    private CodeBaseFingerprint fingerprint;
    private List<URL> urls;
    private boolean needsExploding = false;

    public CodeBase(Configuration config) {
        this.config = config;
        this.codeBaseFile = new File(config.getCodeBaseUri());
        checkArgument(codeBaseFile.exists(), "Code base at " + codeBaseFile + " does not exist");
    }

    public URL[] getUrls() {
        getFingerprint();
        if (needsExploding) {
            throw new UnsupportedOperationException("Exploding WAR or EAR not yet implemented");
        }
        return urls.toArray(new URL[urls.size()]);
    }

    public CodeBaseFingerprint getFingerprint() {
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

        log.debug("Scanned code base at {} in {} ms, fingerprint={}", codeBaseFile, System.currentTimeMillis() - startedAt,
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

    public void initSignatures(CodeBaseScanner codeBaseScanner) {
        long startedAt = System.currentTimeMillis();
        log.info("Scanning code base {}", this);

        codeBaseScanner.getPublicMethodSignatures(this);

        checkState(!signatures.isEmpty(),
                   "Code base at " + codeBaseFile + " does not contain any classes with package prefix " + config.getPackagePrefix());

        writeSignaturesTo(config.getSignatureFile());

        log.debug("Code base {} with package prefix '{}' scanned in {} ms, found {} public methods.",
                  codeBaseFile, config.getPackagePrefix(), System.currentTimeMillis() - startedAt, signatures.size());
    }

    void addSignature(String thisSignature, String declaringSignature) {
        if (declaringSignature != null) {
            if (!declaringSignature.equals(thisSignature)) {
                log.trace("  Adding {} -> {} to overridden signatures", thisSignature, declaringSignature);
                overriddenSignatures.put(thisSignature, declaringSignature);
            }

            if (signatures.add(declaringSignature)) {
                log.trace("  Found {}", declaringSignature);
            }
        }
    }

    private void writeSignaturesTo(File file) {
        PrintWriter out = null;
        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
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

    @VisibleForTesting
    void readScannerResult(File file) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            boolean signaturePhase = true;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    // ignore comment
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
