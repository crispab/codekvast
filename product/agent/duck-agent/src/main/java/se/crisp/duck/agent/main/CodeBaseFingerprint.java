package se.crisp.duck.agent.main;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

import static java.lang.Math.max;

/**
 * @author Olle Hallin
 */
@Value
@Slf4j
public class CodeBaseFingerprint {
    private final int count;
    private final long size;
    private final long lastModified;
    private final int hashCode;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int count;
        private long size;
        private long lastModified;
        private int hashCode;

        public void record(File file) {
            count += 1;
            size += file.length();
            lastModified = max(lastModified, file.lastModified());
            hashCode |= file.hashCode();
            log.trace("Recorded {}, {}", file, this);
        }

        CodeBaseFingerprint build() {
            return new CodeBaseFingerprint(count, size, lastModified, hashCode);
        }
    }
}
