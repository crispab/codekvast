package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorDataEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataReceivedEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Olle Hallin
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final AgentDAO agentDAO;
    private final EventBus eventBus;
    private final Map<Long, Map<String, SignatureEntry>> signatureCache = new ConcurrentHashMap<>();

    @Inject
    public UserServiceImpl(@NonNull UserDAO userDAO, AgentDAO agentDAO, EventBus eventBus) {
        this.userDAO = userDAO;
        this.agentDAO = agentDAO;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void postConstruct() {
        eventBus.register(this);
    }

    @PreDestroy
    public void preDestroy() {
        eventBus.unregister(this);
    }

    /**
     * Translates InputDataReceivedEvent to InputDataUpdatedEvent by eliminating rows that are older than what users only see on their
     * screen.
     */
    @Subscribe
    public void onInvocationDataReceivedEvent(InvocationDataReceivedEvent event) {
        Set<SignatureEntry> newSignatures = new HashSet<>();

        Map<String, SignatureEntry> cache = signatureCache.get(event.getAppId().getOrganisationId());
        if (cache == null) {
            // No user has logged in yet...
            return;
        }

        for (SignatureEntry newEntry : event.getInvocationEntries()) {
            SignatureEntry oldEntry = cache.get(newEntry.getSignature());
            if (oldEntry == null || oldEntry.getInvokedAtMillis() < newEntry.getInvokedAtMillis()) {
                // Inform active users that there is a new invocation...
                newSignatures.add(newEntry);

                // Keep this new high score...
                cache.put(newEntry.getSignature(), newEntry);
            }
        }

        if (!newSignatures.isEmpty()) {
            Collection<String> usernames = userDAO.getInteractiveUsernamesInOrganisation(event.getAppId().getOrganisationId());
            eventBus.post(new InvocationDataUpdatedEvent(event.getAppId(), newSignatures, usernames));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collection<SignatureEntry> getSignatures(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        Set<SignatureEntry> signatures = userDAO.getSignatures(organisationId);
        fillSignatureCache(organisationId, signatures);
        return signatures;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorDataEvent getCollectorDataEvent(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        return agentDAO.createCollectorDataEvent(organisationId);
    }

    private void fillSignatureCache(long organisationId, Set<SignatureEntry> signatures) {
        synchronized (signatureCache) {
            Map<String, SignatureEntry> cache = signatureCache.get(organisationId);
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
                for (SignatureEntry entry : signatures) {
                    cache.put(entry.getSignature(), entry);
                }

                signatureCache.put(organisationId, cache);
            }
        }
    }
}
