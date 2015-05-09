package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.OrganisationSettings;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final AgentDAO agentDAO;
    private final EventBus eventBus;

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

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public WebSocketMessage getWebSocketMessage(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        return agentDAO.createWebSocketMessage(organisationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrganisationSettings(String username, OrganisationSettings organisationSettings) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);

        agentDAO.saveSettings(organisationId, organisationSettings);
        agentDAO.recalculateApplicationStatistics(organisationId);

        eventBus.post(agentDAO.createWebSocketMessage(organisationId));
    }

}
