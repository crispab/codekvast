/*
 * Copyright (c) 2015-2017 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.agent.lib.codebase;

import io.codekvast.agent.lib.model.v1.CodeBaseEntry;
import io.codekvast.agent.lib.model.v1.CodeBasePublication;
import io.codekvast.agent.lib.util.ComputerID;
import io.codekvast.agent.lib.util.Constants;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.v1.MethodSignature;
import io.codekvast.agent.lib.model.v1.SignatureStatus;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Handles a code base, i.e., the set of methods in an application.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ClassWithTooManyFields")
@ToString(of = "codeBaseFiles", includeFieldNames = false)
@EqualsAndHashCode(of = "fingerprint")
@Slf4j
public class CodeBase {

    private static final String ADDED_PATTERNS_FILENAME = "/codekvast/byte-code-added-methods.txt";
    private static final String ENHANCED_PATTERNS_FILENAME = "/codekvast/byte-code-enhanced-methods.txt";

    private static final String SIGNATURES_SECTION = "# Signatures:";
    private static final String OVERRIDDEN_SIGNATURES_SECTION = "# Overridden signatures:";
    static final String RAW_STRANGE_SIGNATURES_SECTION = "# Raw strange signatures:";
    private static final String NORMALIZED_STRANGE_SIGNATURES_SECTION = "# Normalized strange signatures:";

    private final List<File> codeBaseFiles;

    @Getter
    private final CollectorConfig config;

    @Getter
    private final Map<String, MethodSignature> signatures = new TreeMap<>();

    @Getter
    private final Map<String, String> overriddenSignatures = new HashMap<>();

    @Getter
    private final Map<String, SignatureStatus> statuses = new HashMap<>();

    @Getter
    private final CodeBaseFingerprint fingerprint;

    private static final Set<String> strangeSignatures = new TreeSet<>();

    private List<URL> urls;
    private boolean needsExploding = false;

    private final List<Pattern> bytecodeAddedPatterns;
    private final List<Pattern> bytecodeEnhancedPatterns;
    private final Set<Pattern> loggedBadPatterns = new HashSet<>();

    public CodeBase(CollectorConfig config) {
        this.config = config;
        this.codeBaseFiles = config.getCodeBaseFiles();
        this.fingerprint = initUrls();
        this.bytecodeAddedPatterns = readByteCodePatternsFrom(ADDED_PATTERNS_FILENAME);
        this.bytecodeEnhancedPatterns = readByteCodePatternsFrom(ENHANCED_PATTERNS_FILENAME);
    }

    private List<Pattern> readByteCodePatternsFrom(String resourceName) {
        List<Pattern> result = new ArrayList<>();
        log.debug("Reading byte code patterns from {}", resourceName);
        try {
            LineNumberReader reader = new LineNumberReader(
                new BufferedReader(new InputStreamReader(getClass().getResource(resourceName).openStream(), Charset.forName("UTF-8"))));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    addPatternTo(result, resourceName, reader.getLineNumber(), line);
                }
            }
        } catch (Exception e) {
            log.error("Cannot read " + resourceName);
        }
        return result;
    }

    private void addPatternTo(Collection<Pattern> result, String fileName, int lineNumber, String pattern) {
        try {
            result.add(Pattern.compile(pattern));
        } catch (PatternSyntaxException e) {
            log.error("Illegal regexp syntax in {}:{}: {}", fileName, lineNumber, e.toString());
        }
    }

    private String normalizeSignature(MethodSignature methodSignature) {
        return methodSignature == null ? null : normalizeSignature(methodSignature.getAspectjString());
    }

    public String normalizeSignature(String signature) {
        if (signature == null) {
            return null;
        }

        if (isStrangeSignature(signature)) {
            strangeSignatures.add(signature);
        }

        for (Pattern pattern : bytecodeAddedPatterns) {
            if (pattern.matcher(signature).matches()) {
                return null;
            }
        }
        String result = signature.replaceAll(" final ", " ");

        for (Pattern pattern : bytecodeEnhancedPatterns) {
            Matcher matcher = pattern.matcher(result);
            if (matcher.matches()) {
                if (matcher.groupCount() != 3) {
                    logBadPattern(pattern);
                } else {
                    result = matcher.group(1) + "." + matcher.group(2) + matcher.group(3);
                    log.trace("Normalized {} to {}", signature, result);
                    break;
                }
            }
        }

        if (isStrangeSignature(result)) {
            log.warn("Could not normalize {}: {}", signature, result);
        }
        return result;
    }

    boolean isStrangeSignature(String signature) {
        return signature.contains("..") || signature.contains("$$") || signature.contains("CGLIB")
            || signature.contains("EnhancerByGuice") || signature.contains("FastClassByGuice");
    }

    private void logBadPattern(Pattern pattern) {
        if (loggedBadPatterns.add(pattern)) {
            log.error("Expected exactly 3 capturing groups in regexp '{}', ignored.", pattern);
        }
    }


    public URL[] getUrls() {
        if (needsExploding) {
            // TODO: implement WAR and EAR exploding
            throw new UnsupportedOperationException("Exploding WAR or EAR not yet implemented");
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private CodeBaseFingerprint initUrls() {
        long startedAt = System.currentTimeMillis();

        urls = new ArrayList<>();
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

    void addSignature(MethodSignature thisSignature, MethodSignature declaringSignature, SignatureStatus status) {
        String thisNormalizedSignature = normalizeSignature(thisSignature);
        String declaringNormalizedSignature = normalizeSignature(declaringSignature);

        if (declaringNormalizedSignature != null) {
            if (!declaringNormalizedSignature.equals(thisNormalizedSignature) && thisNormalizedSignature != null) {
                log.trace("  Adding {} -> {} to overridden signatures", thisNormalizedSignature, declaringNormalizedSignature);
                overriddenSignatures.put(thisNormalizedSignature, declaringNormalizedSignature);
            } else if (signatures.put(declaringNormalizedSignature, declaringSignature) == null) {
                log.trace("  Found {}", declaringNormalizedSignature);
            }
            statuses.put(declaringNormalizedSignature, status);
        }
    }

    public void writeSignaturesToDisk() {
        File file = config.getSignatureFile(config.getAppName());
        PrintWriter out = null;
        try {
            File directory = file.getAbsoluteFile().getParentFile();
            directory.mkdirs();

            File tmpFile = File.createTempFile("codekvast", ".tmp", directory);
            out = new PrintWriter(tmpFile, "UTF-8");

            out.println(SIGNATURES_SECTION);
            for (String signature : signatures.keySet()) {
                out.printf("%s %s%n", statuses.get(signature), signature);
            }

            out.println();
            out.println("------------------------------------------------------------------------------------------------");
            out.println(OVERRIDDEN_SIGNATURES_SECTION);
            out.println("# child() -> base()");
            for (Map.Entry<String, String> entry : overriddenSignatures.entrySet()) {
                out.printf("%s -> %s%n", entry.getKey(), entry.getValue());
            }

            out.println();
            out.println("------------------------------------------------------------------------------------------------");
            out.println(RAW_STRANGE_SIGNATURES_SECTION);
            for (String signature : strangeSignatures) {
                out.println(signature);
            }

            out.println();
            out.println("------------------------------------------------------------------------------------------------");
            out.println(NORMALIZED_STRANGE_SIGNATURES_SECTION);
            for (String signature : strangeSignatures) {
                String normalized = normalizeSignature(signature);
                if (normalized != null) {
                    out.println(normalized);
                }
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
        return signatures.containsKey(signature);
    }

    public String getBaseSignature(String signature) {
        return signature == null ? null : overriddenSignatures.get(signature);
    }

    boolean isEmpty() {
        return signatures.isEmpty();
    }

    int size() {
        return signatures.size();
    }

    public Collection<CodeBaseEntry> getEntries() {
        List<CodeBaseEntry> result = new ArrayList<>();

        for (Map.Entry<String, MethodSignature> entry : signatures.entrySet()) {
            String name = entry.getKey();
            result.add(new CodeBaseEntry(name, entry.getValue(), statuses.get(name)));
        }

        return result;
    }

    public CodeBasePublication getCodeBasePublication() {
        return CodeBasePublication
            .builder()
            .appName(config.getAppName())
            .appVersion(config.getResolvedAppVersion())
            .collectorVersion(Constants.COLLECTOR_VERSION)
            .computerId(ComputerID.compute().toString())
            .entries(getEntries())
            .hostName(Constants.HOST_NAME)
            .jvmUuid(Constants.JVM_UUID)
            .publishedAtMillis(System.currentTimeMillis())
            .build();
    }
}
