package se.crisp.codekvast.server.codekvast_server.service.impl.reports;

import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author olle.hallin@crisp.se
 */
@Component
class CsvReportFormatter extends ReportFormatter {

    @Override
    ReportService.Format getFormat() {
        return ReportService.Format.CSV;
    }

    @Override
    public String format(MethodUsageReport report) {
        List<String> lines = new ArrayList<>();
        lines.add("# Codekvast Method Usage Report");
        lines.add("# Generated at " + LocalDateTime.now());
        lines.add("#");
        lines.add("# method,application,version,scope,invokedAtMillis (UTC),invokedAt (" + ZoneId.systemDefault().toString() + ")," +
                          "millisBeforeLastCollectorReport");

        report.getMethods().forEach(method -> lines.add(
                String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                              method.getSignature(),
                              method.getApplicationName(),
                              method.getApplicationVersion(),
                              method.getScope(),
                              method.getInvokedAtMillis(),
                              method.getInvokedAtDisplay(),
                              method.getMillisBeforeLastCollectorReport())));

        return lines.stream().collect(Collectors.joining("\n"));
    }
}
