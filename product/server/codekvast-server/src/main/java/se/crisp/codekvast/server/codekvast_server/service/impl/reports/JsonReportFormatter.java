package se.crisp.codekvast.server.codekvast_server.service.impl.reports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@Component
class JsonReportFormatter extends ReportFormatter {

    private final ObjectMapper objectMapper;

    @Inject
    JsonReportFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    ReportService.Format getFormat() {
        return ReportService.Format.JSON;
    }

    @Override
    public String format(MethodUsageReport report) throws CodekvastException {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new CodekvastException("Cannot render report as JSON", e);
        }
    }
}
