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
public class UsageData {
    @NonNull
    @Valid
    private Header header;

    @NonNull
    @Valid
    private Collection<UsageDataEntry> usage;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UsageData(");
        sb.append("header=").append(header);
        sb.append(", usage.size=").append(usage.size());
        sb.append(')');
        return sb.toString();
    }

    public String toLongString() {
        final StringBuilder sb = new StringBuilder("UsageData(");
        sb.append("header=").append(header);
        sb.append(", usage.size=").append(usage.size());
        sb.append(", usage=").append(usage);
        sb.append(')');
        return sb.toString();
    }
}
