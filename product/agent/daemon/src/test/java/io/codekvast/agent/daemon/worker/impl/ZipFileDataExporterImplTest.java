package io.codekvast.agent.daemon.worker.impl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import io.codekvast.agent.daemon.beans.DaemonConfig;
import io.codekvast.agent.daemon.worker.DataExporter;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class ZipFileDataExporterImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DaemonConfig config;

    private DataExporter dataExporter;

    @Before
    public void before() throws Exception {
        config = DaemonConfig.createSampleDaemonConfig()
                             .toBuilder()
                             .exportFile(new File(temporaryFolder.getRoot(), "codekvast-data.zip"))
                             .build();

        dataExporter = new ZipFileDataExporterImpl(jdbcTemplate, config);
    }

    @Test
    public void should_exportData() throws Exception {
        assertThat(config.getExportFile().exists(), is(false));

        dataExporter.exportData();

        //noinspection UseOfSystemOutOrSystemErr
        System.out.println("exportFile = " + config.getExportFile());
        assertThat(config.getExportFile().exists(), is(true));
    }

}
