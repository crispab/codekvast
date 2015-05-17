package se.crisp.codekvast.server.codekvast_server.service.impl.reports;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

/**
 * A report formatter that uses JAXB for rendering XML.
 *
 * @author olle.hallin@crisp.se
 */
@Component
class XmlReportFormatter extends ReportFormatter {

    private final XmlMapper xmlMapper = new XmlMapper();

    XmlReportFormatter() {
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    ReportService.Format getFormat() {
        return ReportService.Format.XML;
    }

    @Override
    public String format(MethodUsageReport report) throws CodekvastException {
        try {
            return xmlMapper.writeValueAsString(report);
        } catch (Exception e) {
            throw new CodekvastException("Cannot render XML", e);
        }
    }
}
