package io.codekvast.common.bootstrap;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author olle.hallin@crisp.se
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final CodekvastCommonSettings settings;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        // @formatter:off
        return registry -> registry.config().commonTags("env", settings.getEnvironment(),
                                                        "CNAME", settings.getDnsCname(),
                                                        "service", settings.getApplicationName());
        // @formatter:on
    }

}
