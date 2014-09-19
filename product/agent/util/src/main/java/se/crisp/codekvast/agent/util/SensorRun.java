package se.crisp.codekvast.agent.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Builder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

/**
 * Data about one run of an app that is instrumented with codekvast-sensor.
 *
 * @author Olle Hallin
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.NONE)
@Builder
public class SensorRun {
    private final String hostName;
    private final UUID uuid;
    private final long startedAtMillis;
    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        FileUtils.writePropertiesTo(file, this, "CodeKvast SensorRun Run");
    }

    public static SensorRun readFrom(File file) throws IOException {
        Properties props = FileUtils.readPropertiesFrom(file);

        return SensorRun.builder()
                        .hostName(props.getProperty("hostName"))
                        .uuid(UUID.fromString(props.getProperty("uuid")))
                        .startedAtMillis(Long.parseLong(props.getProperty("startedAtMillis")))
                        .dumpedAtMillis(Long.parseLong(props.getProperty("dumpedAtMillis")))
                        .build();
    }
}
