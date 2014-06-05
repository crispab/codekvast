package se.crisp.duck.agent.util;

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
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.NONE)
@Builder
public class Sensor {
    private final String appName;
    private final String hostName;
    private final UUID uuid;
    private final long startedAtMillis;
    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        SensorUtils.writePropertiesTo(file, this, "Duck Sensor Run");
    }

    public static Sensor readFrom(File file) throws IOException {
        Properties props = SensorUtils.readPropertiesFrom(file);

        return Sensor.builder()
                     .appName(props.getProperty("appName"))
                     .hostName(props.getProperty("hostName"))
                     .uuid(UUID.fromString(props.getProperty("uuid")))
                     .startedAtMillis(Long.parseLong(props.getProperty("startedAtMillis")))
                     .dumpedAtMillis(Long.parseLong(props.getProperty("dumpedAtMillis")))
                     .build();
    }
}
