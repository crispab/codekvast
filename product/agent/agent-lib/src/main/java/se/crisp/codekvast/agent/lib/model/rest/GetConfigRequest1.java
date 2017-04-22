/*
 * Copyright (c) 2015-2017 Crisp AB
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
package se.crisp.codekvast.agent.lib.model.rest;

import lombok.*;

import javax.validation.constraints.Size;

/**
 * A validated parameter object for getting config from the Codekvast Service.
 *
 * @author olle.hallin@crisp.se
 */
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class GetConfigRequest1 {

    /**
     * What is my license key?
     */
    @NonNull
    @Size(min = 1, message = "licenseKey must be at least 1 characters")
    private final String licenseKey;

    /**
     * What is my app's name?
     */
    @NonNull
    @Size(min = 1, message = "appName must be at least 1 characters")
    private final String appName;

    /**
     * What is my app's version?
     */
    @NonNull
    @Size(min = 1, message = "appVersion must be at least 1 characters")
    private final String appVersion;

    /**
     * Which version of the collector is doing this request?
     */
    @NonNull
    @Size(min = 1, message = "collectorVersion must be at least 1 characters")
    private final String collectorVersion;

    /**
     * What is the name of the host in which the collector executes?
     */
    @NonNull
    @Size(min = 1, message = "hostName must be at least 1 characters")
    private final String hostName;

    /**
     * What is my code base fingerprint?
     */
    @NonNull
    @Size(min = 1, message = "codeBaseFingerprint must be at least 1 characters")
    private final String codeBaseFingerprint;

    public static GetConfigRequest1 sample() {
        return GetConfigRequest1.builder()
                                .appName("appName")
                                .appVersion("appVersion")
                                .codeBaseFingerprint("codeBaseFingerprint")
                                .collectorVersion("collectorVersion")
                                .hostName("hostName")
                                .licenseKey("licenseKey")
                                .build();
    }
}
