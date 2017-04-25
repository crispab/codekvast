package io.codekvast.agent.collector.io;

import io.codekvast.agent.lib.config.CollectorConfig;

/**
 * @author olle.hallin@crisp.se
 */
public interface CodeBasePublisherFactory {
    CodeBasePublisher create(String name, CollectorConfig config);
}
