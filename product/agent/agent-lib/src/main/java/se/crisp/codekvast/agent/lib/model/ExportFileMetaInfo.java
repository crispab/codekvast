/**
 * Copyright (c) 2015 Crisp AB
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

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class ExportFileMetaInfo {

    @NonNull private final String uuid;
    @NonNull private final String schemaVersion;
    @NonNull private final String daemonVersion;
    @NonNull private final String daemonVcsId;
    @NonNull private final String daemonHostname;
    private final String environment;

    public static ExportFileMetaInfo fromInputStream(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);

        return ExportFileMetaInfo.builder()
                                 .uuid(props.getProperty("uuid"))
                                 .schemaVersion(props.getProperty("schemaVersion"))
                                 .daemonVersion(props.getProperty("daemonVersion"))
                                 .daemonVcsId(props.getProperty("daemonVcsId"))
                                 .daemonHostname(props.getProperty("daemonHostname"))
                                 .environment(props.getProperty("environment"))
                                 .build();
    }
}
