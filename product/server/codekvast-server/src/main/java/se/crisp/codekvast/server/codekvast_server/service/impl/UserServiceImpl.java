package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
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

    @Inject
    public UserServiceImpl(@NonNull UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public Collection<InvocationEntry> getSignatures(String username) throws CodekvastException {
        return userDAO.getSignatures(username);
    }

    @Override
    public Collection<Application> getApplications(String username) {
        return userDAO.getApplications(username);
    }

}
