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
package se.crisp.codekvast.agent.daemon.beans;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Encapsulates the configuration of the codekvast-daemon.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@ConfigurationProperties(prefix = "codekvast")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DaemonConfig {
    @NonNull
    private File dataPath;
    @NonNull
    private Integer dataProcessingIntervalSeconds;
    @NonNull
    private String daemonVersion;
    @NonNull
    private String daemonVcsId;
    @NonNull
    private String environment;
    @NonNull
    private File exportFile;
    @NonNull
    private String uploadToPath;
    private String uploadToHost;
    private int uploadToPort = 22;
    private String uploadToUsername;
    private String uploadToPassword;

    private boolean verifyUploadToHostKey;

    public boolean isUploadEnabled() {
        return uploadToHost != null && !uploadToHost.isEmpty() && !uploadToPath.isEmpty();
    }

    public String getDisplayVersion() {
        return daemonVersion + "-" + daemonVcsId;
    }

    public static DaemonConfig createSampleDaemonConfig() {
        return builder()
                .daemonVcsId("daemonVcsId")
                .daemonVersion("daemonVersion")
                .dataPath(new File("dataPath"))
                .dataProcessingIntervalSeconds(600)
                .environment("environment")
                .exportFile(new File("exportFile"))
                .uploadToPath("uploadToPath")
                .build();
    }
}
