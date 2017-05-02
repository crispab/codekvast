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

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "OverlyComplexClass"})
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class CommonPublicationData implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull
    @Size(min = 1)
    private String appName;

    @NonNull
    @Size(min = 1)
    private String appVersion;

    @NonNull
    @Size(min = 1)
    private String codeBaseFingerprint;

    @NonNull
    @Size(min = 1)
    private String collectorVersion;

    @NonNull
    @Size(min = 1)
    private String computerId;

    @NonNull
    private String environment;

    @NonNull
    @Size(min = 1)
    private String excludePackages;

    @NonNull
    @Size(min = 1)
    private String hostName;

    @Min(1_490_000_000_000L)
    private long jvmStartedAtMillis;

    @NonNull
    @Size(min = 1)
    private String jvmUuid;

    @NonNull
    @Size(min = 1)
    private String methodVisibility;

    @NonNull
    @Size(min = 1)
    private String packages;

    @Min(1_490_000_000_000L)
    private long publishedAtMillis;

    @Min(1)
    private int sequenceNumber;

    @NonNull
    private String tags;

    @Override
    public String toString() {
        return String.format(
            "CommonPublicationData{appName='%1$s', appVersion='%2$s', hostName='%3$s', publishedAt=%4$tF:%4$tT%4$tz}",
            appName,
            appVersion,
            hostName,
            publishedAtMillis);
    }

}
