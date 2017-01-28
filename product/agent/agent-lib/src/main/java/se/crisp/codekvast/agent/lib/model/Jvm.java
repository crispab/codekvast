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
package se.crisp.codekvast.agent.lib.model;

import lombok.*;
import lombok.experimental.Wither;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

/**
 * Data about one JVM that is instrumented with codekvast-collector.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Jvm {
    @NonNull
    private String jvmUuid;
    @NonNull
    private CollectorConfig collectorConfig;
    @NonNull
    private String computerId;
    @NonNull
    private String hostName;
    private long startedAtMillis;
    @NonNull
    private String collectorVcsId;
    @NonNull
    private String collectorVersion;
    @Wither
    private long dumpedAtMillis;

    public void saveTo(File file) {
        dumpedAtMillis = System.currentTimeMillis();
        FileUtils.writePropertiesTo(file, this, "Codekvast-instrumented JVM run");
    }

    public static Jvm readFrom(File file) throws IOException {
        Properties props = FileUtils.readPropertiesFrom(file);

        try {
            return builder()
                      .collectorConfig(CollectorConfigFactory.buildCollectorConfig(props))
                      .collectorVcsId(props.getProperty("collectorVcsId"))
                      .collectorVersion(props.getProperty("collectorVersion"))
                      .computerId(props.getProperty("computerId"))
                      .dumpedAtMillis(Long.parseLong(props.getProperty("dumpedAtMillis")))
                      .hostName(props.getProperty("hostName"))
                      .jvmUuid(props.getProperty("jvmUuid"))
                      .startedAtMillis(Long.parseLong(props.getProperty("startedAtMillis")))
                      .build();
        } catch (Exception e) {
            throw new IOException("Cannot parse " + file, e);
        }
    }

    public static Jvm createSampleJvm() {
        return builder()
                .collectorConfig(CollectorConfigFactory.createSampleCollectorConfig())
                .collectorVcsId("collectorVcsId")
                .collectorVersion("collectorVersion")
                .computerId("computerId")
                .dumpedAtMillis(System.currentTimeMillis())
                .hostName("hostName")
                .jvmUuid(UUID.randomUUID().toString())
                .startedAtMillis(System.currentTimeMillis())
                .build();
    }
}
