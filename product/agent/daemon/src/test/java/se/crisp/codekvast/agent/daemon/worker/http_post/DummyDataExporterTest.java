package se.crisp.codekvast.agent.daemon.worker.http_post;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.agent.daemon.worker.DataExportException;
import se.crisp.codekvast.agent.daemon.worker.DataExporter;

import javax.inject.Inject;

@RunWith(SpringJUnit4ClassRunner.class)
@HttpPostIntegrationTest
public class DummyDataExporterTest {

    @Inject
    public DataExporter dataExporter;

    @Test
    public void testExportData_should_do_nothing() throws DataExportException {
        dataExporter.exportData();
    }
}
