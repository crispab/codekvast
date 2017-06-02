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

    @Min(value = 1, message = "customerId must be a positive number")
    private long customerId;

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
    private String agentVersion;

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
    private String hostname;

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
            "CommonPublicationData{customerId=%1$d, appName='%2$s', appVersion='%3$s', hostname='%4$s', publishedAt=%5$tF:%5$tT%5$tz}",
            customerId,
            appName,
            appVersion,
            hostname,
            publishedAtMillis);
    }

}
