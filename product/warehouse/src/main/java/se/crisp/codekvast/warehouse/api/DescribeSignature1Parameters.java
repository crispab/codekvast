package se.crisp.codekvast.warehouse.api;

import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * A validated parameters object for {@link ApiService#describeSignature1(DescribeSignature1Parameters)}
 *
 * @author olle.hallin@crisp.se
 */
@Builder
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DescribeSignature1Parameters {

    public enum OrderBy {INVOKED_AT_ASC, INVOKED_AT_DESC, SIGNATURE}

    /**
     * The signature to search for.
     */
    @NonNull
    @Size(min = 1, message = "signature must be at least 1 characters")
    private final String signature;

    /**
     * How many results to return.
     */
    @Min(value = 1, message = "maxResult must be greater than 0")
    @Max(value = 1000, message = "maxResults must not be higher than 100")
    private final int maxResults;

    /**
     * Exclude methods that have been invoked.
     */
    private final boolean onlyTrulyDeadMethods;

    /**
     * Surround signature with "%" and replace "#" with "."
     */
    private final boolean normalizeSignature;

    /**
     * How to sort the result
     */
    @NonNull
    private final OrderBy orderBy;

    public String getNormalizedSignature() {
        String sig = signature.contains("%") ? signature : "%" + signature + "%";
        return normalizeSignature ? sig.replace("#", ".") : signature;
    }

    public static DescribeSignature1ParametersBuilder defaults() {
        return builder()
                .maxResults(ApiService.Default.MAX_RESULTS)
                .normalizeSignature(ApiService.Default.NORMALIZE_SIGNATURE)
                .onlyTrulyDeadMethods(ApiService.Default.ONLY_TRULY_DEAD_METHODS)
                .orderBy(ApiService.Default.ORDER_BY);
    }

}
