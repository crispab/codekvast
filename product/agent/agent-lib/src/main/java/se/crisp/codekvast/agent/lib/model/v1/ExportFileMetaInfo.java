package se.crisp.codekvast.agent.lib.model.v1;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class ExportFileMetaInfo {

    @NonNull
    private final String uuid;

    @NonNull
    private final String daemonHostname;

    @Wither
    private String fileName;

    @Wither
    private final Long fileLengthBytes;

    public static ExportFileMetaInfo fromInputStream(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);

        return ExportFileMetaInfo.builder()
                                 .uuid(props.getProperty("uuid"))
                                 .daemonHostname(props.getProperty("daemonHostname"))
                                 .build();
    }
}
