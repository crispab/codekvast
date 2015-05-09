package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Collection;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class GetMethodUsageResponse {
    @NonNull
    private final GetMethodUsageRequest request;

    private final int numMethods;
    private final Map<MethodUsageScope, Integer> numMethodsByScope;

    private final Collection<MethodUsageEntry> methods;
}
