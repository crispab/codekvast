package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.server.agent.model.v1.InvocationData;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent.model.v1.JvmData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.event.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import java.util.ArrayList;
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
    public void storeJvmData(JvmData data) throws CodekvastException {
        log.debug("Storing {}", data);

        agentDAO.storeJvmData(data);
    }

    @Override
    public void storeSignatureData(SignatureData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        Collection<InvocationEntry> updatedEntries = agentDAO.storeInvocationData(toInitialInvocationsData(data));
        AppId appId = userDAO.getAppIdByJvmFingerprint(data.getJvmFingerprint());
        eventBus.post(new InvocationDataUpdatedEvent(appId, updatedEntries));
    }

    private InvocationData toInitialInvocationsData(SignatureData signatureData) {
        Collection<InvocationEntry> invocationEntries = new ArrayList<>();
        for (String sig : signatureData.getSignatures()) {
            invocationEntries.add(new InvocationEntry(sig, null, null));
        }
        return InvocationData.builder().header(signatureData.getHeader()).jvmFingerprint(signatureData.getJvmFingerprint()).invocations(
                invocationEntries).build();
    }

    @Override
    public void storeInvocationData(InvocationData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        Collection<InvocationEntry> updatedEntries = agentDAO.storeInvocationData(data);
        AppId appId = userDAO.getAppIdByJvmFingerprint(data.getJvmFingerprint());
        eventBus.post(new InvocationDataUpdatedEvent(appId, updatedEntries));
    }

}
