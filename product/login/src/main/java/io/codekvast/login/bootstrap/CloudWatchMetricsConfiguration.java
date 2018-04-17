/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codekvast.login.bootstrap;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder;
import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.micrometer.cloudwatch.CloudWatchConfig;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure export of metrics to AWS CloudWatch.
 *
 * This class will not be needed once Spring Boot supports auto-configuration also of CloudWatch.
 */
@Configuration
@ConditionalOnProperty(prefix = "management.metrics.export.cloudwatch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CloudWatchMetricsConfiguration {

	@Bean
	public CloudWatchConfig cloudWatchConfig(CloudWatchProperties cloudWatchProperties) {
		return new CloudWatchPropertiesConfigAdapter(cloudWatchProperties);
	}

	@Bean
    public AmazonCloudWatchAsync amazonCloudWatchAsync(CloudWatchProperties cloudWatchProperties) {
        return AmazonCloudWatchAsyncClientBuilder.standard()
                                                 .withRegion(cloudWatchProperties.getAwsRegion())
                                                 .build();
    }

	@Bean
	public CloudWatchMeterRegistry cloudWatchMeterRegistry(CloudWatchConfig cloudWatchConfig, Clock clock, AmazonCloudWatchAsync amazonCloudWatchAsync) {
		return new CloudWatchMeterRegistry(cloudWatchConfig, clock, amazonCloudWatchAsync);
	}

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
        @Value("${spring.application.name}") String applicationName,
        CodekvastCommonSettings codekvastSettings) {
        return registry -> registry.config().commonTags("application", applicationName,
                                                        "host", codekvastSettings.getDnsCname().replaceAll("\\..*$", ""));
    }
}
