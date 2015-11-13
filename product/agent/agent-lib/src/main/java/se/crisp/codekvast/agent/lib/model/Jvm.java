package se.crisp.codekvast.agent.lib.model;

import lombok.*;
import lombok.experimental.Wither;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.util.FileUtils;

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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class Jvm {
    @NonNull
    private String jvmUuid;
    @NonNull
    private CollectorConfig collectorConfig;
    @NonNull
    private String computerId;
    @NonNull
    private String hostName;
    private long startedAtMillis;
    @NonNull
    private String collectorVcsId;
    @NonNull
    private String collectorVersion;
    @Wither
    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        FileUtils.writePropertiesTo(file, this, "Codekvast-instrumented JVM run");
    }

    public static Jvm readFrom(File file) throws IOException {
        Properties props = FileUtils.readPropertiesFrom(file);

        try {
            return Jvm.builder()
                      .collectorConfig(CollectorConfigFactory.buildCollectorConfig(props))
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
