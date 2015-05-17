package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.ReportDAO;
import se.crisp.codekvast.server.codekvast_server.dao.ReportDAO.ReportParameters;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageRequest;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageEntry;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageReport;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageScope;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A service that is responsible for assembling a MethodUsageReport.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserDAO userDAO;
    private final AgentDAO agentDAO;
    private final ReportDAO reportDAO;

    private AtomicInteger nextReportId = new AtomicInteger();
    private final Map<Integer, MethodUsageReport> reports = new HashMap<>();

    @Inject
    public ReportServiceImpl(UserDAO userDAO, AgentDAO agentDAO, ReportDAO reportDAO) {
        this.userDAO = userDAO;
        this.agentDAO = agentDAO;
        this.reportDAO = reportDAO;
    }

    @Override
    @Scheduled(initialDelay = 60_000L, fixedRate = 60_000L)
    public void reportScavenger() {

        synchronized (reports) {
            long now = System.currentTimeMillis();
            for (Iterator<Map.Entry<Integer, MethodUsageReport>> iterator = reports.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Integer, MethodUsageReport> entry = iterator.next();
                MethodUsageReport report = entry.getValue();

                if (report.getReportExpiresAtMillis() <= now) {
                    log.debug("Expiring report {} belonging to {}", report.getReportId(), report.getUsername());
                    iterator.remove();
                }
            }

        }
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public MethodUsageReport getMethodUsagePreview(String username, GetMethodUsageRequest request) throws CodekvastException {
        log.debug("Received {} from {}", request, username);

        long startedAt = System.currentTimeMillis();

        long organisationId = userDAO.getOrganisationIdForUsername(username);

        ReportParameters params =
                ReportParameters.builder()
                                .organisationId(organisationId)
                                .applicationIds(agentDAO.getApplicationIds(organisationId, request.getApplications())
                                                        .stream().map(AppId::getAppId)
                                                        .collect(Collectors.toList()))
                                .jvmIds(reportDAO.getJvmIdsByAppVersions(organisationId, request.getVersions()))
                                .bootstrapSeconds(request.getBootstrapSeconds())
                                .usageCycleSeconds(request.getUsageCycleSeconds())
                                .build();

        List<MethodUsageEntry> methods = new ArrayList<>();

        Map<MethodUsageScope, Integer> numMethodsByScope = new HashMap<>();
        for (MethodUsageScope scope : request.getMethodUsageScopes()) {
            Collection<MethodUsageEntry> methodsForScope = reportDAO.getMethodsForScope(scope, params);
            numMethodsByScope.put(scope, methodsForScope.size());
            methods.addAll(methodsForScope);
        }

        MethodUsageReport report =
                MethodUsageReport.builder()
                                      .request(request)
                                 .username(username)
                                 .reportId(nextReportId.incrementAndGet())
                                 .reportExpiresAtMillis(Instant.now().plusSeconds(600).toEpochMilli())
                                 .methods(methods)
                                      .numMethodsByScope(numMethodsByScope)
                                      .build();

        synchronized (reports) {
            reports.put(report.getReportId(), report);
        }

        // Create a preview copy of the report with only maxPreviewRows methods
        MethodUsageReport response =
                MethodUsageReport.builder()
                                 .request(report.getRequest())
                                 .username(report.getUsername())
                                 .reportId(report.getReportId())
                                 .reportExpiresAtMillis(report.getReportExpiresAtMillis())
                                 .methods(methods.subList(0, Math.min(methods.size(), request.getMaxPreviewRows())))
                                 .numMethodsByScope(report.getNumMethodsByScope())
                                 .build();

        log.debug("Created response to {}'s request for {} in {} ms", username, request, System.currentTimeMillis() - startedAt);
        return response;
    }

    @Override
    public MethodUsageReport getMethodUsageReport(String username, int reportId) {
        synchronized (reports) {
            MethodUsageReport report = reports.get(reportId);

            if (report == null) {
                throw new IllegalArgumentException("Invalid reportId: " + reportId);
            }

            if (!report.getUsername().equals(username)) {
                throw new IllegalArgumentException("No access to report " + reportId);
            }

            log.debug("{} gets report {}", username, reportId);
            return report;
        }
    }
}
