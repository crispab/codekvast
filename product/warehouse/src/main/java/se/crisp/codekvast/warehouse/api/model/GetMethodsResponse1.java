package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;

/**
 * Response to {@link GetMethodsRequest1}.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
@ToString(exclude = "methods")
public class GetMethodsResponse1 {
    /**
     * When was the request received? Millis since epoch.
     */
    private final Long timestamp;

    /**
     * What was the original request?
     */
    private final GetMethodsRequest1 request;

    /**
     * How long did it take to execute the request?
     */
    private final Long queryTimeMillis;

    /**
     * How many methods were found?
     */
    private final int numMethods;

    /**
     * The resulting methods.
     */
    private final List<MethodDescriptor1> methods;
}
