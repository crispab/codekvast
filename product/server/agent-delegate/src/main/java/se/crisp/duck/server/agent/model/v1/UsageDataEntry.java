package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
public class UsageDataEntry {

    /**
     * The used signature was found as-is in the scanned code base.
     */
    public static final int CONFIDENCE_EXACT_MATCH = 0;

    /**
     * The used signature was <em>not</em> found as-is in the scanned code base. It was found however,
     * when searching upwards in the class hierarchy. The reason for not finding it in the first place could be that the method
     * was synthesized at runtime by some bytecode manipulating AOP framework (like Guice).
     */
    public static final int CONFIDENCE_FOUND_IN_PARENT_CLASS = 1;

    /**
     * The used signature was <em>not</em> found at all in the scanned code base. This indicates a problem with the code base scanner,
     * which requires access to the source code to be resolved.
     */
    public static final int CONFIDENCE_NOT_FOUND_IN_CODE_BASE = 2;

    @NonNull
    @Size(min = 1, max = Constraints.MAX_SIGNATURE_LENGTH)
    private String signature;

    private long usedAt;

    @Min(CONFIDENCE_EXACT_MATCH)
    @Max(CONFIDENCE_NOT_FOUND_IN_CODE_BASE)
    private int confidence;
}
