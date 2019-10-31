/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.common.messaging.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * An event that is sent the first time collection data is received.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class CodeBaseReceivedEvent implements CodekvastEvent {
    @NonNull Long customerId;
    @NonNull String appName;
    @NonNull String appVersion;
    @NonNull String agentVersion;
    @NonNull String environment;
    @NonNull String hostname;
    @NonNull Integer size;

    public static CodeBaseReceivedEvent sample() {
        return CodeBaseReceivedEvent.builder()
                                    .customerId(1L)
                                    .appName("appName")
                                    .appVersion("appVersion")
                                    .agentVersion("agentVersion")
                                    .environment("environment")
                                    .hostname("hostname")
                                    .size(17)
                                    .build();
    }
}
