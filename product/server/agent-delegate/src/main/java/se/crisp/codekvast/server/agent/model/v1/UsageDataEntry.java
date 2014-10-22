package se.crisp.codekvast.server.agent.model.v1;

import lombok.*;

import javax.validation.constraints.Size;

/**
 * Data about one used method.
 *
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "signature")
public class UsageDataEntry {
    @NonNull
    @Size(min = 1, max = Constraints.MAX_SIGNATURE_LENGTH)
    private String signature;

    private long usedAtMillis;

    @NonNull
    private UsageConfidence confidence;
}
