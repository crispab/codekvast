package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.OrganisationSettings;
import se.crisp.codekvast.server.codekvast_server.service.StatisticsService;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final AgentDAO agentDAO;
    private final StatisticsService statisticsService;

    @Inject
    public UserServiceImpl(@NonNull UserDAO userDAO, AgentDAO agentDAO, StatisticsService statisticsService) {
        this.userDAO = userDAO;
        this.agentDAO = agentDAO;
        this.statisticsService = statisticsService;
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

        Collection<String> updatedAppNames = agentDAO.saveSettings(organisationId, organisationSettings);
        statisticsService.recalculateApplicationStatistics(organisationId, updatedAppNames);
    }

}
