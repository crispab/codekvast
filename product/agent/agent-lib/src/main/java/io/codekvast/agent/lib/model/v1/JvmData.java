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

/**
 * Data about one instrumented JVM.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ClassWithTooManyFields")
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class JvmData {
    //@formatter:off
    @NonNull private String appName;
    @NonNull private String appVersion;
    @NonNull private String collectorComputerId;
    @NonNull private String collectorHostName;
    @NonNull private Integer collectorResolutionSeconds;
    @NonNull private String collectorVcsId;
    @NonNull private String collectorVersion;
    @NonNull private String daemonComputerId;
    @NonNull private String daemonHostName;
    @NonNull private String daemonVcsId;
    @NonNull private String daemonVersion;
    @NonNull private Long   dumpedAtMillis;
    private String environment;
    @NonNull private String jvmUuid;
    @NonNull private String methodVisibility;
    @NonNull
    private String packages;
    private String excludePackages;
    @NonNull private Long   startedAtMillis;
    @NonNull private String tags;
    //@formatter:on

    public static JvmData createSampleJvmData() {
        return builder()
                .appName("appName")
                .appVersion("appVersion")
                .collectorComputerId("collectorComputerId")
                .collectorHostName("collectorHostName")
                .collectorResolutionSeconds(0)
                .collectorVcsId("collectorVcsId")
                .collectorVersion("collectorVersion")
                .daemonComputerId("daemonComputerId")
                .daemonHostName("daemonHostName")
                .daemonVcsId("daemonVcsId")
                .daemonVersion("daemonVersion")
                .dumpedAtMillis(System.currentTimeMillis())
                .environment("environment")
                .excludePackages("exclude.packages")
                .jvmUuid("jvmUuid")
                .methodVisibility("public")
                .packages("packages")
                .startedAtMillis(System.currentTimeMillis())
                .tags("tags")
                .build();
    }

}
