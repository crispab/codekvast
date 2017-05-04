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
package io.codekvast.javaagent.codebase;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.model.v1.CodeBaseEntry;
import io.codekvast.javaagent.model.v1.CodeBasePublication;
import io.codekvast.javaagent.model.v1.MethodSignature;
import io.codekvast.javaagent.model.v1.SignatureStatus;
import io.codekvast.javaagent.util.SignatureUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Handles a code base, i.e., the set of methods in an application.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
@ToString(of = "codeBaseFiles", includeFieldNames = false)
@EqualsAndHashCode(of = "fingerprint")
@Slf4j
public class CodeBase {

    private final List<File> codeBaseFiles;

    @Getter
    private final AgentConfig config;

    @Getter
    private final Map<String, MethodSignature> signatures = new TreeMap<>();

    @Getter
    private final Map<String, String> overriddenSignatures = new HashMap<>();

    @Getter
    private final Map<String, SignatureStatus> statuses = new HashMap<>();

    @Getter
    private final CodeBaseFingerprint fingerprint;

    private List<URL> urls;
    private boolean needsExploding = false;

    public CodeBase(AgentConfig config) {
        this.config = config;
        this.codeBaseFiles = config.getCodeBaseFiles();
        this.fingerprint = calculateFingerprint();
    }

    URL[] getUrls() {
        if (needsExploding) {
            // TODO: implement WAR and EAR exploding
            throw new UnsupportedOperationException("Exploding WAR or EAR not yet implemented");
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private CodeBaseFingerprint calculateFingerprint() {
        long startedAt = System.currentTimeMillis();

        urls = new ArrayList<>();
        CodeBaseFingerprint.Builder builder = CodeBaseFingerprint.builder(config);
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

        log.debug("Made fingerprint of {} files at {} in {} ms, fingerprint={}", result.getNumFiles(), codeBaseFiles, System.currentTimeMillis() - startedAt,
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
        String thisNormalizedSignature = SignatureUtils.normalizeSignature(thisSignature);
        String declaringNormalizedSignature = SignatureUtils.normalizeSignature(declaringSignature);

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

    boolean isEmpty() {
        return signatures.isEmpty();
    }

    int size() {
        return signatures.size();
    }

    Collection<CodeBaseEntry> getEntries() {
        List<CodeBaseEntry> result = new ArrayList<>();

        for (Map.Entry<String, MethodSignature> entry : signatures.entrySet()) {
            String name = entry.getKey();
            result.add(
                CodeBaseEntry.builder()
                             .normalizedSignature(name)
                             .methodSignature(entry.getValue())
                             .signature(SignatureUtils.stripModifiers(name))
                             .visibility(SignatureUtils.getVisibility(name))
                             .signatureStatus(statuses.get(name))
                             .build());
        }

        return result;
    }

    public CodeBasePublication getCodeBasePublication(int sequenceNumber) {
        return CodeBasePublication
            .builder()
            .commonData(config.commonPublicationDataBuilder()
                              .codeBaseFingerprint(getFingerprint().getSha256())
                              .sequenceNumber(sequenceNumber)
                              .build())
            .entries(getEntries())
            .overriddenSignatures(new HashMap<>(overriddenSignatures))
            .strangeSignatures(SignatureUtils.getStrangeSignatureMap())
            .build();
    }

}
