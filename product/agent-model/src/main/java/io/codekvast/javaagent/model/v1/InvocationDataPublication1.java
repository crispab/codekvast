/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.javaagent.model.v1;

import lombok.*;

import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Set;

/**
 * Output of the InvocationDataPublisher implementations.
 *
 * @author olle.hallin@crisp.se
 * @deprecated Use {@link io.codekvast.javaagent.model.v2.InvocationDataPublication2} instead.
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Deprecated
public class InvocationDataPublication1 implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull
    private CommonPublicationData1 commonData;

    @NonNull
    private Set<String> invocations;

    @Min(1_490_000_000_000L)
    private long recordingIntervalStartedAtMillis;

    @Override
    public String toString() {
        return String.format(
            "InvocationDataPublication1{commonData=%1$s, invocations.size()=%2$d, recordingIntervalStartedAt=%3$tF:%3$tT%3$tz}",
            commonData, invocations.size(), recordingIntervalStartedAtMillis);
    }

}
