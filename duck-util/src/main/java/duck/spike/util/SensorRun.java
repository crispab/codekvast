package duck.spike.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
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
@Builder
public class SensorRun {

    private static final String HOST_NAME_KEY = "hostName";
    private static final String APP_NAME_KEY = "appName";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String UUID_KEY = "uuid";
    private static final String STARTED_AT_MILLIS_KEY = "startedAtMillis";
    private static final String OUTPUT_AT_MILLIS_KEY = "outputAtMillis";

    private final String hostName;
    private final String appName;
    private final String environment;
    private final UUID uuid;
    private final long startedAtMillis;
    private long outputAtMillis;

    public void saveTo(File file) {
        Properties props = new Properties();
        props.put(HOST_NAME_KEY, hostName);
        props.put(APP_NAME_KEY, appName);
        props.put(ENVIRONMENT_KEY, environment);
        props.put(UUID_KEY, uuid.toString());
        props.put(STARTED_AT_MILLIS_KEY, Long.toString(startedAtMillis));
        props.put(OUTPUT_AT_MILLIS_KEY, Long.toString(outputAtMillis));

        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            props.store(new BufferedOutputStream(new FileOutputStream(file)), "Duck Sensor Run");
            out.flush();
            out.close();
        } catch (IOException e) {
            System.err.println("Cannot write " + file + ": " + e);
        }
    }

    @SneakyThrows(IOException.class)
    public static SensorRun readFrom(File file) {

        Properties props = new Properties();
        props.load(new BufferedInputStream(new FileInputStream(file)));

        return SensorRun.builder()
                        .hostName(props.getProperty(HOST_NAME_KEY))
                        .appName(props.getProperty(APP_NAME_KEY))
                        .environment(props.getProperty(ENVIRONMENT_KEY))
                        .uuid(UUID.fromString(props.getProperty(UUID_KEY)))
                        .startedAtMillis(Long.parseLong(props.getProperty(STARTED_AT_MILLIS_KEY)))
                        .outputAtMillis(Long.parseLong(props.getProperty(OUTPUT_AT_MILLIS_KEY)))
                        .build();
    }
}
