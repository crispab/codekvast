package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Builder;
import lombok.Value;

import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class GetMethodUsageResponse {
    int allMethods;
    int matchedMethods;

    int deadMethods;
    int probablyDeadInvoked;
    int bootMethods;
    int liveMethods;
    int rowLimit;

    Collection<MethodUsageEntry> methods;
}
