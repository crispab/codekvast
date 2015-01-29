package se.crisp.codekvast.server.agent_api.model.v1;

import lombok.*;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * REST data about one instrumented JVM.
 * 
 * Should be uploaded regularly during the lifetime of a JVM.
 *
 * @author Olle Hallin
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
    @Size(max = Constraints.MAX_TAGS_LENGTH)
    private String tags;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_HOST_NAME_LENGTH)
    private String hostName;

    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_COMPUTER_ID_LENGTH)
    private String computerId;

    @NonNull
    @NotBlank
    @Size(min = Constraints.MIN_FINGERPRINT_LENGTH, max = Constraints.MAX_FINGERPRINT_LENGTH)
    private String jvmFingerprint;

    private long startedAtMillis;
    private long dumpedAtMillis;

    @NonNull
    @Size(max = Constraints.MAX_CODEKVAST_VERSION_LENGTH)
    @Pattern(regexp = "[a-zA-Z0-9.-_]+")
    private String codekvastVersion;


    @NonNull
    @Size(max = Constraints.MAX_CODEKVAST_VCS_ID_LENGTH)
    @Pattern(regexp = "[a-zA-Z0-9]+")
    private String codekvastVcsId;
}
