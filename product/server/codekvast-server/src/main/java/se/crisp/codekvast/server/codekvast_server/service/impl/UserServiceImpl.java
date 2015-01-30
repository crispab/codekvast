package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.Application;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final AgentDAO agentDAO;

    @Inject
    public UserServiceImpl(@NonNull UserDAO userDAO, AgentDAO agentDAO) {
        this.userDAO = userDAO;
        this.agentDAO = agentDAO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collection<InvocationEntry> getSignatures(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        return userDAO.getSignatures(organisationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collection<Application> getApplications(String username) throws CodekvastException {
        long organisationId = userDAO.getOrganisationIdForUsername(username);
        return userDAO.getApplications(organisationId);
    }

}
