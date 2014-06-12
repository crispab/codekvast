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
        final StringBuilder sb = new StringBuilder("SignatureData(");
        sb.append("header=").append(header);
        sb.append(", signatures.size=").append(signatures.size());
        sb.append(')');
        return sb.toString();
    }

    public String toLongString() {
        final StringBuilder sb = new StringBuilder("SignatureData(");
        sb.append("header=").append(header);
        sb.append(", signatures.size=").append(signatures.size());
        sb.append(", signatures=").append(signatures);
        sb.append(')');
        return sb.toString();
    }
}
