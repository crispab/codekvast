package se.crisp.codekvast.server.agent_api.model.v1;

import lombok.*;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;

/**
 * REST data about one instrumented JVM.
 * 
 * Should be uploaded regularly during the lifetime of a JVM.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JvmData {
    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_APP_NAME_LENGTH)
    private String appName;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_APP_VERSION_LENGTH)
    private String appVersion;

    @NonNull
    @NotBlank
    @Size(min = Constraints.MIN_FINGERPRINT_LENGTH, max = Constraints.MAX_FINGERPRINT_LENGTH)
    private String jvmUuid;

    @NonNull
    @Size(max = Constraints.MAX_TAGS_LENGTH)
    private String tags;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_HOST_NAME_LENGTH)
    private String collectorHostName;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_COMPUTER_ID_LENGTH)
    private String collectorComputerId;

    private int collectorResolutionSeconds;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_METHOD_EXECUTION_POINTCUT_LENGTH)
    private String methodExecutionPointcut;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_HOST_NAME_LENGTH)
    private String agentHostName;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_COMPUTER_ID_LENGTH)
    private String agentComputerId;

    @NonNull
    @Size(max = Constraints.MAX_CODEKVAST_VERSION_LENGTH)
    private String codekvastVersion;


    @NonNull
    @Size(max = Constraints.MAX_CODEKVAST_VCS_ID_LENGTH)
    private String codekvastVcsId;

    private long startedAtMillis;
    private long dumpedAtMillis;
}
