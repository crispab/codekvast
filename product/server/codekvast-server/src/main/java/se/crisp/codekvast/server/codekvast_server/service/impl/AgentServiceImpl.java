package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;
import se.crisp.codekvast.server.codekvast_server.service.StatisticsService;

import javax.inject.Inject;

/**
 * The implementation of the AgentService.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final AgentDAO agentDAO;
    private final UserDAO userDAO;
    private final StatisticsService statisticsService;

    @Inject
    public AgentServiceImpl(AgentDAO agentDAO, UserDAO userDAO, StatisticsService statisticsService) {
        this.agentDAO = agentDAO;
        this.userDAO = userDAO;
        this.statisticsService = statisticsService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeJvmData(String apiAccessID, JvmData data) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(apiAccessID);
        long appId = agentDAO.getAppId(organisationId, data.getAppName());

        agentDAO.storeJvmData(organisationId, appId, data);
        statisticsService.recalculateApplicationStatistics(agentDAO.getAppIdByJvmUuid(data.getJvmUuid()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeSignatureData(SignatureData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        AppId appId = agentDAO.getAppIdByJvmUuid(data.getJvmUuid());
        if (appId == null) {
            log.info("Ignoring invocation data for JVM {}", data.getJvmUuid());
            return;
        }

        agentDAO.storeInvocationData(appId, data);
        statisticsService.recalculateApplicationStatistics(appId);
    }

}
