package io.codekvast.agent.collector.io;

import io.codekvast.agent.lib.config.CollectorConfig;

/**
 * @author olle.hallin@crisp.se
 */
public interface InvocationDataPublisherFactory {

    InvocationDataPublisher create(String name, CollectorConfig config);
}
