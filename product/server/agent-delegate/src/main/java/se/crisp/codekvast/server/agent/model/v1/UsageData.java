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
public class UsageData {
    @NonNull
    @Valid
    private Header header;

    @NonNull
    @Valid
    private Collection<UsageDataEntry> usage;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(header=" + header + ", usage.size=" + usage.size() + ')';
    }

    public String toLongString() {
        return getClass().getSimpleName() + "(header=" + header + ", usage.size=" + usage.size() + ", usage=" + usage + ')';
    }
}
