package se.crisp.codekvast.agent.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Builder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Data about one run of an app that is instrumented with codekvast-sensor.
 *
 * @author Olle Hallin
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.NONE)
@Builder
public class JvmRun {
    private final String hostName;
    private final String jvmFingerprint;
    private final long startedAtMillis;
    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        FileUtils.writePropertiesTo(file, this, "CodeKvast-instrumented JVM run");
    }

    public static JvmRun readFrom(File file) throws IOException {
        Properties props = FileUtils.readPropertiesFrom(file);

        return JvmRun.builder()
                        .hostName(props.getProperty("hostName"))
                        .jvmFingerprint(props.getProperty("jvmFingerprint"))
                        .startedAtMillis(Long.parseLong(props.getProperty("startedAtMillis")))
                        .dumpedAtMillis(Long.parseLong(props.getProperty("dumpedAtMillis")))
                        .build();
    }
}
