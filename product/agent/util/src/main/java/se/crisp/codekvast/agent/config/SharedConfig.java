package se.crisp.codekvast.agent.config;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;
import se.crisp.codekvast.agent.util.ConfigUtils;

import java.io.File;
import java.util.Properties;

/**
 * Base class for agent side configuration objects.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Value
@Builder
public class SharedConfig implements CodekvastConfig {
    @NonNull
    private final String customerName;

    @NonNull
    private final File dataPath;

    @NonNull
    private final String packagePrefix;

    protected static SharedConfig buildSharedConfig(Properties props) {
        return builder()
                .dataPath(new File(ConfigUtils.getOptionalStringValue(props, "dataPath", System.getProperty("java.io.tmpdir") +
                        "/codekvast")))
                .customerName(ConfigUtils.getMandatoryStringValue(props, "customerName"))
                .packagePrefix(ConfigUtils.getMandatoryStringValue(props, "packagePrefix"))
                .build();
    }

    public static SharedConfig buildSampleSharedConfig() {
        return builder()
                .dataPath(new File(ConfigUtils.SAMPLE_DATA_PATH))
                .customerName("Customer Name")
                .packagePrefix("sample.").build();
    }

    protected File myDataPath(String appName) {
        return new File(dataPath, ConfigUtils.getNormalizedChildPath(customerName, appName));
    }

    public String getNormalizedPackagePrefix() {
        return ConfigUtils.getNormalizedPackagePrefix(packagePrefix);
    }
}
