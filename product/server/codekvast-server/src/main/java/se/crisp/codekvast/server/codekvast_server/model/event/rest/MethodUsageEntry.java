package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Builder;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class MethodUsageEntry {
    String signature;
    String applicationName;
    String applicationVersion;
    String scope;
    long invokedAtMillis;
    String invokedAtDisplay;
    long millisBeforeLastCollectorReport;
}
