package se.crisp.codekvast.warehouse.api.model;

import lombok.*;
import se.crisp.codekvast.warehouse.api.ApiService;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * A validated parameters object for {@link ApiService#getMethods(GetMethodsRequest1)}
 *
 * @author olle.hallin@crisp.se
 */
@Builder
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class GetMethodsRequest1 {

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
    @Max(value = 10_000, message = "maxResults must be less than or equal to 10000")
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

    public static GetMethodsRequest1Builder defaults() {
        return builder()
                .maxResults(ApiService.DEFAULT_MAX_RESULTS)
                .normalizeSignature(ApiService.DEFAULT_NORMALIZE_SIGNATURE)
                .onlyTrulyDeadMethods(ApiService.DEFAULT_ONLY_TRULY_DEAD_METHODS)
                .orderBy(ApiService.DEFAULT_ORDER_BY);
    }

}
