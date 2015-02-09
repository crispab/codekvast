package se.crisp.codekvast.agent.config;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import se.crisp.codekvast.agent.util.ConfigUtils;

import java.io.File;
import java.util.Properties;

/**
 * Base class for agent side configuration objects.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class SharedConfig implements CodekvastConfig {
    @NonNull
    private final File dataPath;

    protected static SharedConfig buildSharedConfig(Properties props) {
        File tmpDir = new File("/tmp");
        if (!tmpDir.isDirectory()) {
            tmpDir = new File(System.getProperty("java.io.tmpdir"));
        }
        String defaultValue = new File(tmpDir, "codekvast").getAbsolutePath();

        return builder()
                .dataPath(new File(ConfigUtils.getOptionalStringValue(props, "dataPath", defaultValue)))
                .build();
    }

    public static SharedConfig buildSampleSharedConfig() {
        return builder()
                .dataPath(new File(ConfigUtils.SAMPLE_DATA_PATH))
                .build();
    }
}
