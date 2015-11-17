package se.crisp.codekvast.agent.lib.model;

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
                                                           .daemonHostname("some-hostname")
                                                           .daemonVcsId("daemon Vcs:Id")
                                                           .daemonVersion("daemon V1")
                                                           .schemaVersion("V1")
                                                           .uuid("uuid-value")
                                                           .build(),
                                         lines);

        assertThat(lines, hasItems(
                "daemonHostname = some-hostname",
                "daemonVcsId = daemon Vcs\\:Id",
                "daemonVersion = daemon V1",
                "schemaVersion = V1",
                "uuid = uuid-value"));
    }
}