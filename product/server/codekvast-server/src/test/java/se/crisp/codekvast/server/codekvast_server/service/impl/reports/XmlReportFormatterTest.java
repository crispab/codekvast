package se.crisp.codekvast.server.codekvast_server.service.impl.reports;

import org.junit.Test;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageRequest;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageEntry;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageScope;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author olle.hallin@crisp.se
 */
public class XmlReportFormatterTest {

    private ReportFormatter formatter = new XmlReportFormatter();

    @Test
    public void testRenderReport() throws Exception {

        GetMethodUsageRequest request = GetMethodUsageRequest
                .builder()
                .applications(asList("app1", "app2"))
                .bootstrapSeconds(60)
                .maxPreviewRows(100)
                .methodUsageScopes(asList(MethodUsageScope.values()))
                .usageCycleSeconds(3600)
                .versions(asList("1.0", "1.1"))
                .build();

        Map<MethodUsageScope, Integer> methodsByScope = new HashMap<>();
        methodsByScope.put(MethodUsageScope.DEAD, 1000);

        List<MethodUsageEntry> methods = asList(
                MethodUsageEntry.builder()
                                .signature("signature1")
                                .invokedAtDisplay("")
                                .invokedAtMillis(0L)
                                .scope("Dead")
                                .build()
        );

        MethodUsageReport report = MethodUsageReport
                .builder()
                .reportCreatedAt("report-created-at")
                .reportExpiresAtMillis(Instant.now().plusSeconds(60).toEpochMilli())
                .reportId(1)
                .availableFormats(asList(ReportService.Format.values()))
                .timeZone("time-zone")
                .username("username")
                .request(request)
                .numMethodsByScope(methodsByScope)
                .methods(methods)
                .build();

        String formatted = formatter.format(report);
        assertThat(formatted, notNullValue());
    }
}
