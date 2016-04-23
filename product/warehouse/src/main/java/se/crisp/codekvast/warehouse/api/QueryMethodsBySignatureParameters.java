package se.crisp.codekvast.warehouse.api;

import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

/**
 * A validated parameters object for {@link QueryService#queryMethodsBySignature(QueryMethodsBySignatureParameters)}
 *
 * @author olle.hallin@crisp.se
 */
@Builder
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryMethodsBySignatureParameters {

    public enum OrderBy {INVOKED_AT_ASC, INVOKED_AT_DESC, SIGNATURE}

    /**
     * The signature to search for.
     */
    @NonNull
    @Size(min = 5)
    private final String signature;

    /**
     * How many results to return.
     */
    @Max(1000)
    private final int maxResults;

    /**
     * Exclude methods that have been invoked.
     */
    private final boolean onlyTrulyDeadMethods;

    /**
     * Surround signature with "%" and reoṕlace "#" with "."
     */
    private final boolean normalizeSignature;

    /**
     * How to sort the result
     */
    @NonNull
    private final OrderBy orderBy;

    public String getNormalizedSignature() {
        String sig = "%" + signature + "%";
        return normalizeSignature ? sig.replace("%%", "%").replace("#", ".") : signature;
    }

    public static QueryMethodsBySignatureParametersBuilder defaults() {
        return builder()
                .maxResults(QueryService.Default.MAX_RESULTS)
                .normalizeSignature(QueryService.Default.NORMALIZE_SIGNATURE)
                .onlyTrulyDeadMethods(QueryService.Default.ONLY_TRULY_DEAD_METHODS)
                .orderBy(QueryService.Default.ORDER_BY);
    }

}
