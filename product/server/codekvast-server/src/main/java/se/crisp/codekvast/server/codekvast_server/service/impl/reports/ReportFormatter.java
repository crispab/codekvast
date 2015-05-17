package se.crisp.codekvast.server.codekvast_server.service.impl.reports;

import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

/**
 * @author olle.hallin@crisp.se
 */
abstract class ReportFormatter {
    abstract ReportService.Format getFormat();

    abstract String format(MethodUsageReport report) throws CodekvastException;
}
