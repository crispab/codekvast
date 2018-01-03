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
package io.codekvast.dashboard.webapp.model.methods;

import io.codekvast.dashboard.webapp.WebappService;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * A validated parameters object for {@link WebappService#getMethods(GetMethodsRequest)}
 *
 * @author olle.hallin@crisp.se
 */
@Builder(toBuilder = true)
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class GetMethodsRequest {

    /**
     * The signature to search for.
     */
    @NonNull
    @Size(min = 1, message = "signature must be at least 1 characters")
    private final String signature;

    /**
     * How many results to return.
     */
    @Min(value = 1, message = "maxResult must be greater than 0")
    @Max(value = 10_000, message = "maxResults must be less than or equal to 10000")
    private final int maxResults;

    /**
     * Exclude methods that have been invoked.
     */
    private final boolean onlyTrulyDeadMethods;

    /**
     * Surround signature with "%" and replace "#" with "."
     */
    private final boolean normalizeSignature;


    public String getNormalizedSignature() {
        String sig = signature.contains("%") ? signature : "%" + signature + "%";
        return normalizeSignature ? sig.replace("#", ".") : signature;
    }

    public static GetMethodsRequest defaults() {
        return builder()
            .signature("")
            .maxResults(WebappService.DEFAULT_MAX_RESULTS)
            .normalizeSignature(WebappService.DEFAULT_NORMALIZE_SIGNATURE)
            .onlyTrulyDeadMethods(WebappService.DEFAULT_ONLY_TRULY_DEAD_METHODS)
            .build();
    }

}
