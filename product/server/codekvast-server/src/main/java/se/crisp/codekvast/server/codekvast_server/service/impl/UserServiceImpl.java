package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;

/**
 * @author Olle Hallin
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;

    @Inject
    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public boolean isUnique(UniqueKind kind, String value) {
        int count;
        switch (kind) {
        case USERNAME:
            count = userDAO.countUsersByUsername(value);
            break;
        case CUSTOMER_NAME:
            count = userDAO.countCustomersByName(value);
            break;
        default:
            throw new IllegalArgumentException("Unknown kind: " + kind);
        }
        boolean result = count == 0;

        log.debug("Is {} '{}' unique: {}", kind, value, result);
        return result;
    }
}
