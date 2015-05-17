package se.crisp.codekvast.server.codekvast_server.service.impl.reports;

import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

/**
 * @author olle.hallin@crisp.se
 */
@Component
class JsonReportFormatter extends ReportFormatter {

    @Override
    ReportService.Format getFormat() {
        return ReportService.Format.JSON;
    }

    @Override
    public String format(MethodUsageReport report) {
        // TODO: implement JsonReportFormatter
        return "";
    }
}
