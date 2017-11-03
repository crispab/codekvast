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
 * @deprecated Use {@link io.codekvast.javaagent.model.v2.CommonPublicationData2} instead.
 */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "OverlyComplexClass"})
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Deprecated
public class CommonPublicationData1 implements Serializable {
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
            "%1$s(customerId=%2$d, appName='%3$s', appVersion='%4$s', hostname='%5$s', publishedAt=%6$tF:%6$tT%6$tz)",
            this.getClass().getSimpleName(),
            customerId,
            appName,
            appVersion,
            hostname,
            publishedAtMillis);
    }

    public static CommonPublicationData1 sampleCommonPublicationData() {
        return builder()
            .agentVersion("agentVersion")
            .appName("appName")
            .appVersion("appVersion")
            .codeBaseFingerprint("codeBaseFingerprint")
            .computerId("computerId")
            .customerId(1L)
            .environment("environment")
            .excludePackages("excludePackages1, excludePackages2")
            .hostname("hostname")
            .jvmStartedAtMillis(1509461136162L)
            .jvmUuid("jvmUuid")
            .methodVisibility("methodVisibility")
            .packages("packages1, packages2")
            .publishedAtMillis(1509461136162L)
            .sequenceNumber(1)
            .tags("tags")
            .build();
    }

}
