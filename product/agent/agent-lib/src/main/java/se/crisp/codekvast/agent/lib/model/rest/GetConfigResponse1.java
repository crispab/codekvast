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
import se.crisp.codekvast.agent.lib.io.CodeBasePublisher;

/**
 * A response object for {@link GetConfigRequest1}
 *
 * @author olle.hallin@crisp.se
 */
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class GetConfigResponse1 {

    /**
     * Either the nick-name or the fully qualified class name of the implementation of {@link CodeBasePublisher} to use.
     * Each implementation defines it's own nick name.
     */
    @NonNull
    private final String codeBasePublisherClass;

    /**
     * The configuration of the {@link CodeBasePublisher} to use, coded as
     * semicolon-separated list of key=value pairs.
     * Neither keys nor values may contains semicolons, space or tab characters.
     *
     * It is up to the specified codeBasePublisherClass to parse the config.
     */
    @NonNull
    private final String codeBasePublisherConfig;

    /**
     * How often shall the codebase be re-scanned for changes?
     * Defaults to 600 seconds.
     */
    @Builder.Default
    private final int codeBasePublisherCheckIntervalSeconds = 600;

    /**
     * How often shall a failed codebase publishing be retried?
     * Defaults to 600 seconds.
     */
    @Builder.Default
    private final int codeBasePublisherRetryIntervalSeconds = 600;

    public static GetConfigResponse1 sample() {
        return builder()
            .codeBasePublisherConfig("codeBasePublisherConfig")
            .codeBasePublisherClass("codeBasePublisherClass")
            .build();
    }
}
