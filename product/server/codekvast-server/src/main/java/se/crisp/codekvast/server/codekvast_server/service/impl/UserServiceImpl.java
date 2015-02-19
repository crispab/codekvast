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
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorStatusMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDataMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.internal.InvocationDataReceivedEvent;
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
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final AgentDAO agentDAO;
    private final EventBus eventBus;
    private final Map<Long, Map<String, SignatureDisplay>> signatureCache = new ConcurrentHashMap<>();

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
        Set<SignatureDisplay> newSignatures = new HashSet<>();

        Map<String, SignatureDisplay> cache = signatureCache.get(event.getAppId().getOrganisationId());
        if (cache == null) {
            // No user has logged in yet...
            return;
        }

        for (SignatureEntry entry : event.getInvocationEntries()) {
            SignatureDisplay oldEntry = cache.get(entry.getSignature());
            if (oldEntry == null || oldEntry.getInvokedAtMillis() < entry.getInvokedAtMillis()) {
                // Inform active users that there is a new invocation...
                SignatureDisplay newEntry = SignatureDisplay.builder()
                                                            .name(entry.getSignature())
                                                            .invokedAtMillis(entry.getInvokedAtMillis())
                                                            .millisSinceJvmStart(entry.getMillisSinceJvmStart())
                                                            .build();
                newSignatures.add(newEntry);

                // Keep this new high score...
                cache.put(newEntry.getName(), newEntry);
            }
        }

        if (!newSignatures.isEmpty()) {
            Collection<String> usernames = userDAO.getInteractiveUsernamesInOrganisation(event.getAppId().getOrganisationId());
            eventBus.post(SignatureDataMessage.builder()
                                              .usernames(usernames)
                                              .appId(event.getAppId())
                                              .signatures(newSignatures)
                                              .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collection<SignatureDisplay> getSignatures(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        Set<SignatureDisplay> signatures = userDAO.getSignatures(organisationId);
        fillSignatureCache(organisationId, signatures);
        return signatures;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectorStatusMessage getCollectorStatusMessage(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        return agentDAO.createCollectorStatusMessage(organisationId);
    }

    private void fillSignatureCache(long organisationId, Set<SignatureDisplay> signatures) {
        synchronized (signatureCache) {
            Map<String, SignatureDisplay> cache = signatureCache.get(organisationId);
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
                for (SignatureDisplay entry : signatures) {
                    cache.put(entry.getName(), entry);
                }

                signatureCache.put(organisationId, cache);
            }
        }
    }
}
