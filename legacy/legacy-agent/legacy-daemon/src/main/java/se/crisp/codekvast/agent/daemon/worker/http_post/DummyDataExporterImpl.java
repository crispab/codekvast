package se.crisp.codekvast.agent.daemon.worker.http_post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.daemon.worker.DataExportException;
import se.crisp.codekvast.agent.daemon.worker.DataExporter;

/**
 * A dummy implementation of DataExporter that does nothing. In the HTTP POST profile, data is continuously uploaded to a central data
 * warehouse.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Profile("httpPost")
@Slf4j
public class DummyDataExporterImpl implements DataExporter {

    @Override
    public void exportData() throws DataExportException {
        log.trace("Data export not supported in {} profile", "httpPost");
    }
}
