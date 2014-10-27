package se.crisp.codekvast.agent.util;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.io.File;

/**
 * Base class for agent side configuration objects.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Value
@Builder
public class SharedConfig {
    @NonNull
    private final String customerName;

    @NonNull
    private final String appName;

    @NonNull
    private final File dataPath;
    @NonNull
    private final String packagePrefix;

    public File getUsageFile() {
        return new File(myDataPath(), "usage.dat");
    }

    protected File myDataPath() {
        return new File(dataPath, ConfigUtils.getNormalizedChildPath(customerName, appName));
    }

    public File getJvmRunFile() {
        return new File(myDataPath(), "jvm-run.dat");
    }

    public String getNormalizedPackagePrefix() {
        return ConfigUtils.getNormalizedPackagePrefix(packagePrefix);
    }
}
