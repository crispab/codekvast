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
    Collection<MethodUsageEntry> methods;
}
