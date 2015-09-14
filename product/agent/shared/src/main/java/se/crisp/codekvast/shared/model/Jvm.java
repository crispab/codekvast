package se.crisp.codekvast.shared.model;

import lombok.*;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Data about one JVM that is instrumented with codekvast-collector.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class Jvm {
    @NonNull
    private final String jvmUuid;
    @NonNull
    private final CollectorConfig collectorConfig;
    @NonNull
    private final String computerId;
    @NonNull
    private final String hostName;
    private final long startedAtMillis;
    @NonNull
    private final String collectorVcsId;
    @NonNull
    private final String collectorVersion;

    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        FileUtils.writePropertiesTo(file, this, "Codekvast-instrumented JVM run");
    }

    public static Jvm readFrom(File file) throws IOException {
        Properties props = FileUtils.readPropertiesFrom(file);

        try {
            return Jvm.builder()
                      .collectorConfig(CollectorConfig.buildCollectorConfig(props))
                      .collectorVcsId(props.getProperty("collectorVcsId"))
                      .collectorVersion(props.getProperty("collectorVersion"))
                      .computerId(props.getProperty("computerId"))
                      .dumpedAtMillis(Long.parseLong(props.getProperty("dumpedAtMillis")))
                      .hostName(props.getProperty("hostName"))
                      .jvmUuid(props.getProperty("jvmUuid"))
                      .startedAtMillis(Long.parseLong(props.getProperty("startedAtMillis")))
                      .build();
        } catch (Exception e) {
            throw new IOException("Cannot parse " + file, e);
        }
    }
}
