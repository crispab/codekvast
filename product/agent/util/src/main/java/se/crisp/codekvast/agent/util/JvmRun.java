package se.crisp.codekvast.agent.util;

import lombok.*;
import lombok.experimental.Builder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

/**
 * Data about one run of an app that is instrumented with codekvast-collector.
 *
 * @author Olle Hallin
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.NONE)
@Builder
public class JvmRun {
    @NonNull
    private final SharedConfig sharedConfig;
    @NonNull
    private final String hostName;
    @NonNull
    private final String appName;
    @NonNull
    private final String appVersion;
    @NonNull
    private final String jvmFingerprint;
    @NonNull
    private final URI codeBaseUri;
    private final long startedAtMillis;

    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        FileUtils.writePropertiesTo(file, this, "Codekvast-instrumented JVM run");
    }

    public static JvmRun readFrom(File file) throws IOException {
        Properties props = FileUtils.readPropertiesFrom(file);

        return JvmRun.builder()
                     .sharedConfig(SharedConfig.buildSharedConfig(props))
                     .hostName(props.getProperty("hostName"))
                     .appName(props.getProperty("appName"))
                     .appVersion(props.getProperty("appVersion"))
                     .jvmFingerprint(props.getProperty("jvmFingerprint"))
                     .codeBaseUri(ConfigUtils.getMandatoryUriValue(props, "codeBaseUri", false))
                     .startedAtMillis(Long.parseLong(props.getProperty("startedAtMillis")))
                     .dumpedAtMillis(Long.parseLong(props.getProperty("dumpedAtMillis")))
                     .build();
    }
}
