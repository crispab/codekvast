package se.crisp.codekvast.agent.model;

import lombok.*;
import lombok.experimental.Builder;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Data about one run of an app that is instrumented with codekvast-collector.
 *
 * @author Olle Hallin
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.NONE)
@Builder
public class Jvm {
    @NonNull
    private final CollectorConfig collectorConfig;
    @NonNull
    private final String hostName;
    @NonNull
    private final String jvmFingerprint;

    private final long startedAtMillis;
    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        FileUtils.writePropertiesTo(file, this, "Codekvast-instrumented JVM run");
    }

    public static Jvm readFrom(File file) throws IOException {
        Properties props = FileUtils.readPropertiesFrom(file);

        return Jvm.builder()
                  .collectorConfig(CollectorConfig.buildCollectorConfig(props))
                  .hostName(props.getProperty("hostName"))
                  .jvmFingerprint(props.getProperty("jvmFingerprint"))
                  .startedAtMillis(Long.parseLong(props.getProperty("startedAtMillis")))
                  .dumpedAtMillis(Long.parseLong(props.getProperty("dumpedAtMillis")))
                  .build();
    }
}
