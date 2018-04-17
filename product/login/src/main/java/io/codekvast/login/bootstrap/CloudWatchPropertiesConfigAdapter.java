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

import io.micrometer.cloudwatch.CloudWatchConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

import java.time.Duration;

/**
 * Adapter to convert {@link CloudWatchProperties} to a {@link CloudWatchConfig}.
 *
 * @author Jon Schneider
 */
class CloudWatchPropertiesConfigAdapter extends PropertiesConfigAdapter<CloudWatchProperties>
		implements CloudWatchConfig {

	CloudWatchPropertiesConfigAdapter(CloudWatchProperties properties) {
		super(properties);
	}

	@Override
	public String get(String key) {
        return null;
	}

	@Override
	public String namespace() {
        return super.get(CloudWatchProperties::getNamespace, CloudWatchConfig.super::namespace);
	}

	@Override
	public boolean enabled() {
        return super.get(CloudWatchProperties::isEnabled, CloudWatchConfig.super::enabled);
	}

	@Override
	public Duration step() {
		return get(CloudWatchProperties::getStep, CloudWatchConfig.super::step);
	}
}
