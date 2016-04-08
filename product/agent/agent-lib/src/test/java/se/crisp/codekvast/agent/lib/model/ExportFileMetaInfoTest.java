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

        Set<String> lines = new HashSet<String>();

        FileUtils.extractFieldValuesFrom(
                ExportFileMetaInfo.createSampleExportFileMetaInfo()
                                  .toBuilder()
                                  .uuid("uuid-value")
                                  .daemonVcsId("daemon Vcs:Id")
                                  .build(), lines);

        assertThat(lines, hasItems(
                "daemonHostname = daemonHostname",
                "daemonVcsId = daemon Vcs\\:Id",
                "daemonVersion = daemonVersion",
                "schemaVersion = 1",
                "environment = environment",
                "uuid = uuid-value"));
    }
}
