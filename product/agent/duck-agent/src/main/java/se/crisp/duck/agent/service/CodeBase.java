package se.crisp.duck.agent.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@ToString(of = "codeBaseFile", includeFieldNames = false)
@EqualsAndHashCode(of = "fingerprint")
@Slf4j
public class CodeBase {

    @ToString
    @EqualsAndHashCode
    public static class Fingerprint {
        private int count;
        private long size;
        private long lastModified;

        private void record(File file) {
            count += 1;
            size += file.length();
            lastModified = max(lastModified, file.lastModified());
            log.trace("Recorded {}, {}", file, this);
        }
    }

    private final File codeBaseFile;
    private List<URL> urls;
    @Getter
    private Fingerprint fingerprint;
    private boolean needsExploding = false;

    public CodeBase(String codeBasePath) {
        this.codeBaseFile = new File(codeBasePath);
        checkArgument(codeBaseFile.exists(), "Code base at " + codeBaseFile + " does not exist");

        init();
    }

    public URL[] getUrls() {
        if (needsExploding) {
            throw new UnsupportedOperationException("Exploding WAR or EAR not yet implemented");
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private void init() {
        long startedAt = System.currentTimeMillis();

        urls = new ArrayList<>();
        fingerprint = new Fingerprint();

        if (codeBaseFile.isDirectory()) {
            addUrl(codeBaseFile);
            traverse(codeBaseFile.listFiles());
        } else if (codeBaseFile.getName().endsWith(".jar")) {
            fingerprint.record(codeBaseFile);
            addUrl(codeBaseFile);
        } else if (codeBaseFile.getName().endsWith(".war")) {
            fingerprint.record(codeBaseFile);
            needsExploding = true;
        } else if (codeBaseFile.getName().endsWith(".ear")) {
            fingerprint.record(codeBaseFile);
            needsExploding = true;
        }
        log.debug("Scanned code base at {} in {} ms, fingerprint={}", codeBaseFile, System.currentTimeMillis() - startedAt, fingerprint);
    }

    @SneakyThrows(MalformedURLException.class)
    private void addUrl(File file) {
        log.trace("Adding URL {}", file);
        urls.add(file.toURI().toURL());
    }

    private void traverse(File[] files) {
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    fingerprint.record(file);
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    fingerprint.record(file);
                    addUrl(file);
                } else if (file.isDirectory()) {
                    traverse(file.listFiles());
                }
            }
        }
    }

}
