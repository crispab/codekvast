package duck.spike.util;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Builder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Olle Hallin
 */
@Value
@Builder
public class Configuration {
    private final String appName;
    private final URI codeBaseUri;
    private final String packagePrefix;
    private final File dataPath;
    private final int sensorDumpIntervalSeconds;
    private final int warehouseUploadIntervalSeconds;
    private final URI warehouseUri;

    public File getDataFile() {
        return new File(dataPath, "duck-usage.txt");
    }

    @SneakyThrows(URISyntaxException.class)
    public static Configuration parseConfigFile(String configFile) {

        // TODO: read properties from configFile

        return Configuration.builder()
                            .appName("Crisp Sample App")
                            .codeBaseUri(new URI("jar:file:../sample-app/build/libs/sample-app.jar!/"))
                            .packagePrefix("se.crisp")
                            .sensorDumpIntervalSeconds(10)
                            .dataPath(new File(System.getProperty("java.io.tmpdir"), "duck"))
                            .warehouseUploadIntervalSeconds(5)
                            .warehouseUri(new URI("http://localhost:8180"))
                            .build();
    }
}
