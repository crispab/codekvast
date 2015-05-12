package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.ReportDAO;
import se.crisp.codekvast.server.codekvast_server.dao.ReportDAO.ReportParameters;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageRequest;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageResponse;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageEntry;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageScope;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A service that is responsible for assembling a GetMethodUsageResponse.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserDAO userDAO;
    private final AgentDAO agentDAO;
    private final ReportDAO reportDAO;

    @Inject
    public ReportServiceImpl(UserDAO userDAO, AgentDAO agentDAO, ReportDAO reportDAO) {
        this.userDAO = userDAO;
        this.agentDAO = agentDAO;
        this.reportDAO = reportDAO;
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public GetMethodUsageResponse getMethodUsage(String username, GetMethodUsageRequest request) throws CodekvastException {
        log.debug("Received {} from {}", request, username);

        long startedAt = System.currentTimeMillis();

        ReportParameters params =
                ReportParameters.builder()
                                .organisationId(userDAO.getOrganisationIdForUsername(username))
                                .applicationIds(agentDAO.getApplicationIds(userDAO.getOrganisationIdForUsername(username),
                                                                           request.getApplications()).stream().map(AppId::getAppId)
                                                        .collect(Collectors.toList()))
                                .jvmIds(reportDAO.getJvmIdsByAppVersions(userDAO.getOrganisationIdForUsername(username),
                                                                         request.getVersions()))
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

        GetMethodUsageResponse response =
                GetMethodUsageResponse.builder()
                                      .request(request)
                                      .methods(methods.subList(0, Math.min(methods.size(), request.getMaxPreviewRows())))
                                      .numMethodsByScope(numMethodsByScope)
                                      .build();

        log.debug("Created response to {}'s request for {} in {} ms", username, request, System.currentTimeMillis() - startedAt);
        return response;
    }
}
