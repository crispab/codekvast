package se.crisp.codekvast.agent.lib.model.v1;

import org.junit.Test;
import se.crisp.codekvast.agent.lib.util.FileUtils;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class ExportFileMetaInfoTest {

    @Test
    public void testConvertToLines() throws Exception {

        Set<String> lines = new HashSet<>();
        FileUtils.extractFieldValuesFrom(ExportFileMetaInfo.builder()
                                                           .uuid("uuid-value")
                                                           .daemonHostname("some-hostname")
                                                           .build(), lines);

        assertThat(lines, hasItems("uuid = uuid-value", "daemonHostname = some-hostname"));
    }
}