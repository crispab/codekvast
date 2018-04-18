/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
