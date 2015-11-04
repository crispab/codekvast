package se.crisp.codekvast.daemon.impl.local_warehouse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import se.crisp.codekvast.daemon.DataExporter;
import se.crisp.codekvast.daemon.beans.DaemonConfig;

import java.io.File;
import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalWarehouseDataExporterImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DaemonConfig config;

    private File exportFile;

    private DataExporter dataExporter;

    @Before
    public void before() throws Exception {
        exportFile = new File("/tmp", "codekvast-export.zip");
        exportFile.delete();

        // TODO exportFile = new File(temporaryFolder.getRoot(), "codekvast-export.zip");

        config = DaemonConfig.builder()
                             .apiAccessID("apiAccessID")
                             .apiAccessSecret("apiSecret")
                             .daemonVcsId("daemonVcsId")
                             .daemonVersion("daemonVersion")
                             .dataPath(new File("foobar"))
                             .dataProcessingIntervalSeconds(600)
                             .environment(getClass().getSimpleName())
                             .exportFile(exportFile)
                             .serverUri(new URI("http://foobar"))
                             .build();

        dataExporter = new LocalWarehouseDataExporterImpl(jdbcTemplate, config);
    }

    @Test
    public void testExportData() throws Exception {
        assertThat(exportFile.exists(), is(false));

        dataExporter.exportData();

        System.out.println("exportFile = " + exportFile);
        assertThat(exportFile.exists(), is(true));
    }
}
