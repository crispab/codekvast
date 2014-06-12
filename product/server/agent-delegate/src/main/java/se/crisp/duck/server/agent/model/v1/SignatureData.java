package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.Valid;
import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignatureData {
    @NonNull
    @Valid
    private Header header;

    @NonNull
    private Collection<String> signatures;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(header=" + header + ", signatures.size=" + signatures.size() + ')';
    }

    public String toLongString() {
        return getClass().getSimpleName() + "(header=" + header + ", signatures.size=" + signatures
                .size() + ", signatures=" + signatures + ')';
    }
}
