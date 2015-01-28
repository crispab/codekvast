package se.crisp.codekvast.server.codekvast_server.dao;

import lombok.Value;
import lombok.experimental.Builder;

/**
 * The "uptime" for an organisation's all collectors
 *
 * @author Olle Hallin
 */
@Value
@Builder
public class CollectorTimestamp {
    private final long startedAtMillis;
    private final long dumpedAtMillis;
}
