package se.crisp.codekvast.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.Valid;
import java.util.Collection;

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
    @Valid
    private Header header;

    private String jvmFingerprint;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @NonNull
    @Valid
    private Collection<InvocationEntry> invocations;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(header=" + header + ", jvmFingerprint=" + jvmFingerprint + ", invocations.size=" + invocations
                .size() + ')';
    }

    public String toLongString() {
        return getClass().getSimpleName() + "(header=" + header + ", jvmFingerprint=" + jvmFingerprint + ", invocations.size=" + invocations
                .size() + ", invocations=" + invocations + ')';
    }
}
