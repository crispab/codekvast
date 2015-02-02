package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataReceivedEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.Application;
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
    private final EventBus eventBus;
    private final Map<Long, Map<String, InvocationEntry>> signatureCache = new ConcurrentHashMap<>();

    @Inject
    public UserServiceImpl(@NonNull UserDAO userDAO, EventBus eventBus) {
        this.userDAO = userDAO;
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
        Set<InvocationEntry> newSignatures = new HashSet<>();

        Map<String, InvocationEntry> cache = signatureCache.get(event.getAppId().getOrganisationId());
        if (cache == null) {
            // No user has logged in yet...
            return;
        }

        for (InvocationEntry newEntry : event.getInvocationEntries()) {
            InvocationEntry oldEntry = cache.get(newEntry.getSignature());
            if (oldEntry == null || oldEntry.getInvokedAtMillis() < newEntry.getInvokedAtMillis()) {
                // Inform active users that there is a new invocation...
                newSignatures.add(newEntry);

                // Keep this new high score...
                cache.put(newEntry.getSignature(), newEntry);
            }
        }

        if (!newSignatures.isEmpty()) {
            Collection<String> usernames = userDAO.getUsernamesInOrganisation(event.getAppId().getOrganisationId());
            eventBus.post(new InvocationDataUpdatedEvent(event.getAppId(), newSignatures, usernames));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collection<InvocationEntry> getSignatures(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        Set<InvocationEntry> signatures = userDAO.getSignatures(organisationId);
        fillSignatureCache(organisationId, signatures);
        return signatures;
    }

    private void fillSignatureCache(long organisationId, Set<InvocationEntry> signatures) {
        synchronized (signatureCache) {
            Map<String, InvocationEntry> cache = signatureCache.get(organisationId);
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
                for (InvocationEntry entry : signatures) {
                    cache.put(entry.getSignature(), entry);
                }

                signatureCache.put(organisationId, cache);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collection<Application> getApplications(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        return userDAO.getApplications(organisationId);
    }

}
