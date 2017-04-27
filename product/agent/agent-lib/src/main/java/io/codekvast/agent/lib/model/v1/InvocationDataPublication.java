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
package io.codekvast.agent.lib.model.v1;

import lombok.*;

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
    @NonNull
    private String appName;
    @NonNull
    private String appVersion;
    @NonNull
    private String codeBaseFingerprint;
    @NonNull
    private String collectorVersion;
    @NonNull
    private String computerId;
    @NonNull
    private String environment;
    @NonNull
    private String hostName;
    @NonNull
    Set<String> invocations;
    private long jvmStartedAtMillis;
    @NonNull
    private String jvmUuid;
    private int publicationCount;
    private long publishedAtMillis;
    private long recordingIntervalStartedAtMillis;
    @NonNull
    private String tags;

    @Override
    public String toString() {
        return String.format("InvocationDataPublication{appName='%1$s'" +
                                 ", appVersion='%2$s'" +
                                 ", hostName='%3$s'" +
                                 ", publication=#%4$d" +
                                 ", interval=[%5$tF:%5$tT%5$tz--%6$tF:%6$tT%6$tz]" +
                                 ", invocations.size()=%7$d}",
                             appName,
                             appVersion,
                             hostName,
                             publicationCount,
                             recordingIntervalStartedAtMillis,
                             publishedAtMillis,
                             invocations.size()
        );
    }
}
