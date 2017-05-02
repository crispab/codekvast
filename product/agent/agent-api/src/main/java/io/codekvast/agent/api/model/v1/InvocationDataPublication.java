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
package io.codekvast.agent.api.model.v1;

import io.codekvast.agent.api.util.PublishingUtils;
import lombok.*;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Set;

/**
 * Output of the InvocationDataPublisher implementations.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class InvocationDataPublication implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull
    private CommonPublicationData commonData;

    @NonNull
    private Set<String> invocations;

    @Min(1_490_000_000_000L)
    private long recordingIntervalStartedAtMillis;

    @Override
    public String toString() {
        return String.format(
            "InvocationDataPublication{commonData=%s, , recordingIntervalStartedAt=%2$tF:%2$tT%2$tz, invocations.size()=%3$d}",
            commonData, recordingIntervalStartedAtMillis, invocations.size());
    }

    @SuppressWarnings("unused")
    @AssertTrue
    public boolean isValid() {
        boolean result = true;
        for (String signature : invocations) {
            if (!PublishingUtils.isValid(signature)) {
                result = false;
            }
        }
        return result;
    }
}
