package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorDataEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataReceivedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;

/**
 * The implementation of the AgentService.
 *
 * @author Olle Hallin
 */
@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final EventBus eventBus;
    private final AgentDAO agentDAO;
    private final UserDAO userDAO;

    @Inject
    public AgentServiceImpl(EventBus eventBus, AgentDAO agentDAO, UserDAO userDAO) {
        this.eventBus = eventBus;
        this.agentDAO = agentDAO;
        this.userDAO = userDAO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeJvmData(String apiAccessID, JvmData data) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(apiAccessID);
        long appId = agentDAO.getAppId(organisationId, data.getAppName(), data.getAppVersion());

        agentDAO.storeJvmData(organisationId, appId, data);
        CollectorDataEvent event = agentDAO.createCollectorUpTimeEvent(organisationId);
        eventBus.post(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeSignatureData(SignatureData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        AppId appId = userDAO.getAppIdByJvmUuid(data.getJvmUuid());
        if (appId == null) {
            log.info("Ignoring invocation data for JVM {}", data.getJvmUuid());
            return;
        }
        SignatureData storedData = agentDAO.storeInvocationData(appId, data);
        InvocationDataReceivedEvent event = new InvocationDataReceivedEvent(appId, storedData.getSignatures());
        eventBus.post(event);
    }

}
