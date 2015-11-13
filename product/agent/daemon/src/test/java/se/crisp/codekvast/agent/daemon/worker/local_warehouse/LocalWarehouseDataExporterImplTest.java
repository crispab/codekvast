package se.crisp.codekvast.agent.daemon.worker.local_warehouse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.worker.DataExporter;

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

    private DataExporter dataExporter;

    @Before
    public void before() throws Exception {
        File exportFile = new File("/tmp", "codekvast-export.zip");
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
    public void should_exportData() throws Exception {
        assertThat(config.getExportFile().exists(), is(false));

        dataExporter.exportData();

        System.out.println("exportFile = " + config.getExportFile());
        assertThat(config.getExportFile().exists(), is(true));
    }

    @Test
    public void should_not_exportData_when_exportFile_is_null() throws Exception {
        assertThat(config.getExportFile().exists(), is(false));

        config = config.withExportFile(null);
        dataExporter = new LocalWarehouseDataExporterImpl(jdbcTemplate, config);

        dataExporter.exportData();
    }

    @Test
    public void should_not_exportData_when_exportFile_is_not_ending_with_zip() throws Exception {
        assertThat(config.getExportFile().exists(), is(false));

        config = config.withExportFile(new File("foo.bar"));
        dataExporter = new LocalWarehouseDataExporterImpl(jdbcTemplate, config);

        dataExporter.exportData();

        assertThat(config.getExportFile().exists(), is(false));
    }
}
