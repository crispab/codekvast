package se.crisp.codekvast.server.agent_api.model.v1;

import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * REST data about used signatures.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignatureData {
    @NonNull
    @NotBlank
    @Size(min = Constraints.MIN_JVM_UUID_LENGTH, max = Constraints.MAX_FINGERPRINT_LENGTH)
    private String jvmUuid;

    private long agentTimeMillis;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @NonNull
    @Valid
    private List<SignatureEntry> signatures;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(jvmUuid=" + jvmUuid + ", signatures.size=" + signatures
                .size() + ')';
    }

    public String toLongString() {
        return getClass().getSimpleName() + "(jvmUuid=" + jvmUuid + ", signatures.size=" + signatures
                .size() + ", signatures=" + signatures + ')';
    }
}
