package se.crisp.codekvast.agent.lib.model.v1;

import lombok.*;

/**
 * Data about one instrumented JVM.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class JvmData {
    //@formatter:off
    @NonNull private String appName;
    @NonNull private String appVersion;
    @NonNull private String collectorComputerId;
    @NonNull private String collectorHostName;
    @NonNull private Integer collectorResolutionSeconds;
    @NonNull private String collectorVcsId;
    @NonNull private String collectorVersion;
    @NonNull private String daemonComputerId;
    @NonNull private String daemonHostName;
    @NonNull private String daemonVcsId;
    @NonNull private String daemonVersion;
    @NonNull private Integer dataProcessingIntervalSeconds;
    @NonNull private Long   dumpedAtMillis;
    @NonNull private String jvmUuid;
    @NonNull private String methodVisibility;
    @NonNull private Long   startedAtMillis;
    @NonNull private String tags;
    //@formatter:on
}
