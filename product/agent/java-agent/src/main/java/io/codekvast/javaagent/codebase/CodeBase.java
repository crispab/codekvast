/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.javaagent.codebase;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.model.v3.CodeBaseEntry3;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import io.codekvast.javaagent.model.v3.MethodSignature3;
import io.codekvast.javaagent.util.SignatureUtils;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.java.Log;

/**
 * Handles a code base, i.e., the set of methods in an application.
 *
 * @author olle.hallin@crisp.se
 */
// @SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
@ToString(of = {"codeBaseFiles", "fingerprint"})
@EqualsAndHashCode(of = "fingerprint")
@Log
public class CodeBase {

  private final List<File> codeBaseFiles;

  @Getter private final AgentConfig config;

  @Getter private final Set<MethodSignature3> signatures = new HashSet<>();

  @Getter private final CodeBaseFingerprint fingerprint;

  private List<URL> urls;
  private boolean needsExploding = false;

  public CodeBase(AgentConfig config) {
    this.config = config;
    this.codeBaseFiles = detectWebApp(config.getCodeBaseFiles());
    this.fingerprint = calculateFingerprint();
  }

  private List<File> detectWebApp(List<File> codeBaseFiles) {
    List<File> result = new ArrayList<>();

    for (File file : codeBaseFiles) {
      File webInf = file;
      if (!webInf.getName().equals("WEB-INF")) {
        webInf = new File(webInf, "WEB-INF");
      }

      if (webInf.exists() && webInf.isDirectory()) {
        File classes = new File(webInf, "classes/");
        File lib = new File(webInf, "lib");
        if (classes.exists() && classes.isDirectory() && lib.exists() && lib.isDirectory()) {
          // replace file with classes + lib
          result.add(classes);
          result.add(lib);
        }
      } else {
        result.add(file);
      }
    }
    return result;
  }

  URL[] getUrls() {
    if (needsExploding) {
      // TODO: implement WAR and EAR exploding
      throw new UnsupportedOperationException("Exploding WAR or EAR not yet implemented");
    }
    return urls.toArray(new URL[0]);
  }

  private CodeBaseFingerprint calculateFingerprint() {
    long startedAt = System.currentTimeMillis();

    urls = new ArrayList<>();
    CodeBaseFingerprint.Builder builder = CodeBaseFingerprint.builder(config);
    for (File file : codeBaseFiles) {
      if (file.isDirectory()) {
        if (containsAnyClassFile(file)) {
          addUrl(makeSureBasenameEndsWithSlash(file));
        }
        traverse(builder, file.listFiles());
      } else {
        String name = file.getName();
        if (name.endsWith(".jar")) {
          builder.record(file);
          addUrl(file);
        } else if (name.endsWith(".war") || name.endsWith(".ear")) {
          builder.record(file);
          needsExploding = true;
        }
      }
    }

    CodeBaseFingerprint result = builder.build();

    logger.fine(
        String.format(
            "Made fingerprint of %d class files and %d jar files at %s in %d ms, fingerprint=%s",
            result.getNumClassFiles(),
            result.getNumJarFiles(),
            codeBaseFiles,
            System.currentTimeMillis() - startedAt,
            result));
    return result;
  }

  private File makeSureBasenameEndsWithSlash(File file) {
    // A URLClassLoader does not load raw class files from a directory unless the url ends with a
    // slash
    return file.getName().endsWith("/") ? file : new File(file.getParent(), file.getName() + "/");
  }

  @SneakyThrows(MalformedURLException.class)
  private void addUrl(File file) {
    logger.finest("Adding URL " + file);
    urls.add(file.toURI().toURL());
  }

  private boolean containsAnyClassFile(File dir) {
    for (File file : dir.listFiles()) {
      if (file.isFile() && file.getName().endsWith(".class")) {
        return true;
      }
      if (file.isDirectory() && containsAnyClassFile(file)) {
        return true;
      }
    }
    return false;
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

  void addSignature(MethodSignature3 signature) {
    String normalizedSignature = SignatureUtils.normalizeSignature(signature);

    if (normalizedSignature != null && signatures.add(signature)) {
      logger.finest("  Found " + normalizedSignature);
    }
  }

  boolean isEmpty() {
    return signatures.isEmpty();
  }

  int size() {
    return signatures.size();
  }

  Collection<CodeBaseEntry3> getEntries() {
    List<CodeBaseEntry3> result = new ArrayList<>();

    for (MethodSignature3 signature : signatures) {
      result.add(
          CodeBaseEntry3.builder()
              .methodSignature(signature)
              .signature(SignatureUtils.stripModifiers(signature.getAspectjString()))
              .visibility(SignatureUtils.getVisibility(signature.getAspectjString()))
              .build());
    }
    return result;
  }

  public CodeBasePublication3 getCodeBasePublication(long customerId, int sequenceNumber) {
    return CodeBasePublication3.builder()
        .commonData(
            config.commonPublicationData().toBuilder()
                .codeBaseFingerprint(getFingerprint().toString())
                .customerId(customerId)
                .sequenceNumber(sequenceNumber)
                .build())
        .entries(getEntries())
        .build();
  }
}
