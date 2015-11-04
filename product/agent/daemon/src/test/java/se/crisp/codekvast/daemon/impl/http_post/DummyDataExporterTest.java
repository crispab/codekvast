package se.crisp.codekvast.daemon.impl.http_post;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.daemon.DataExportException;
import se.crisp.codekvast.daemon.DataExporter;
import se.crisp.codekvast.daemon.main.HttpPostIntegrationTest;

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
