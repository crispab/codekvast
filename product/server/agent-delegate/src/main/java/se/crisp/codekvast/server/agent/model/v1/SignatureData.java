package se.crisp.codekvast.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;
import java.util.Collection;

/**
 * REST signature data from one code base.
 * 
 * Should be uploaded to the codekvast-server each time the code base is changed.
 *
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignatureData {
    @NonNull
    @NotBlank
    @Size(min = Constraints.MIN_FINGERPRINT_LENGTH, max = Constraints.MAX_FINGERPRINT_LENGTH)
    private String jvmFingerprint;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @NonNull
    private Collection<String> signatures;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(jvmFingerprint=" + jvmFingerprint + ", signatures.size=" + signatures
                .size() + ')';
    }

    public String toLongString() {
        return getClass().getSimpleName() + "(jvmFingerprint=" + jvmFingerprint + ", signatures.size=" + signatures
                .size() + ", signatures=" + signatures + ')';
    }
}
