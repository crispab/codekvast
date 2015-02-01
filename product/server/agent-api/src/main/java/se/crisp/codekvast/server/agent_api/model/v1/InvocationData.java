package se.crisp.codekvast.server.agent_api.model.v1;

import lombok.*;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * REST data about used signatures.
 *
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InvocationData {
    @NonNull
    @NotBlank
    @Size(min = Constraints.MIN_FINGERPRINT_LENGTH, max = Constraints.MAX_FINGERPRINT_LENGTH)
    private String jvmFingerprint;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @NonNull
    @Valid
    private List<InvocationEntry> invocations;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(jvmFingerprint=" + jvmFingerprint + ", invocations.size=" + invocations
                .size() + ')';
    }

    public String toLongString() {
        return getClass().getSimpleName() + "(jvmFingerprint=" + jvmFingerprint + ", invocations.size=" + invocations
                .size() + ", invocations=" + invocations + ')';
    }
}
