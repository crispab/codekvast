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

import com.amazonaws.SDKGlobalConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;

/**
 * {@link ConfigurationProperties} for configuring CloudWatch metrics export.
 *
 */
@Component
@ConfigurationProperties(prefix = "management.metrics.export.cloudwatch")
@Data
public class CloudWatchProperties {

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

    private String namespace;

    private boolean enabled;

    private int batchSize = 20;

    private String awsAccessKeyId;
    private String awsSecretKey;
    private String awsRegion;

    /**
     * Publish AWS credentials as system properties (if present) so that AmazonCloudWatchAsyncClient can find them.
     */
    @PostConstruct
    public void setAwsSystemPropertiesIfPresent() {
        if (awsAccessKeyId != null && !awsAccessKeyId.trim().isEmpty()) {
            System.setProperty(SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY, awsAccessKeyId);
        }
        if (awsSecretKey != null && !awsSecretKey.trim().isEmpty()) {
            System.setProperty(SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY, awsSecretKey);
        }
    }
}
