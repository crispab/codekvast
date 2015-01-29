package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationData;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.CollectorTimestamp;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorUptimeEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import java.util.Collection;

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
        long appId = userDAO.getAppId(organisationId, data.getAppName(), data.getAppVersion());

        agentDAO.storeJvmData(organisationId, appId, data);

        postCollectorUptimeEvent(organisationId);
    }

    private void postCollectorUptimeEvent(long organisationId) {
        Collection<String> usernames = userDAO.getUsernamesInOrganisation(organisationId);
        CollectorTimestamp timestamp = agentDAO.getCollectorTimestamp(organisationId);
        CollectorUptimeEvent event = new CollectorUptimeEvent(timestamp, usernames);
        log.debug("Posting {}", event);
        eventBus.post(event);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeInvocationData(InvocationData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        AppId appId = userDAO.getAppIdByJvmFingerprint(data.getJvmFingerprint());
        if (appId == null) {
            log.info("Ignoring invocation data for JVM {}", data.getJvmFingerprint());
            return;
        }

        Collection<InvocationEntry> updatedEntries = agentDAO.storeInvocationData(appId, data);
        eventBus.post(new InvocationDataUpdatedEvent(appId, updatedEntries));
    }

}
