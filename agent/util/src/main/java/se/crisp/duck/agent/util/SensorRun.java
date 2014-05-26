package se.crisp.duck.agent.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Builder;

import java.io.*;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Olle Hallin
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class SensorRun {

    private static final String HOST_NAME_KEY = "hostName";
    private static final String UUID_KEY = "uuid";
    private static final String STARTED_AT_MILLIS_KEY = "startedAtMillis";
    private static final String SAVED_AT_MILLIS_KEY = "savedAtMillis";

    private final String hostName;
    private final UUID uuid;
    private final long startedAtMillis;

    private long savedAtMillis;

    public void saveTo(File file) {
        savedAtMillis = System.currentTimeMillis();

        Properties props = new Properties();
        props.put(HOST_NAME_KEY, hostName);
        props.put(UUID_KEY, uuid.toString());
        props.put(STARTED_AT_MILLIS_KEY, Long.toString(startedAtMillis));
        props.put(SAVED_AT_MILLIS_KEY, Long.toString(savedAtMillis));

        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            props.store(new BufferedOutputStream(new FileOutputStream(file)), "Duck Sensor Run");
            out.flush();
            out.close();
        } catch (IOException e) {
            System.err.println("Cannot write " + file + ": " + e);
        }
    }

    public static SensorRun readFrom(File file) throws IOException {

        Properties props = new Properties();
        props.load(new BufferedInputStream(new FileInputStream(file)));

        return SensorRun.builder()
                        .hostName(props.getProperty(HOST_NAME_KEY))
                        .uuid(UUID.fromString(props.getProperty(UUID_KEY)))
                        .startedAtMillis(Long.parseLong(props.getProperty(STARTED_AT_MILLIS_KEY)))
                        .savedAtMillis(Long.parseLong(props.getProperty(SAVED_AT_MILLIS_KEY)))
                        .build();
    }
}
