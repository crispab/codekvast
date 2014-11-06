package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import java.util.Locale;

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

    private String normalizeName(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public boolean isUnique(UniqueKind kind, String name) {
        int count;
        String normalizedName = normalizeName(name);

        switch (kind) {
        case USERNAME:
            count = userDAO.countUsersByUsername(normalizedName);
            break;
        case CUSTOMER_NAME:
            count = userDAO.countCustomersByNameLc(normalizedName);
            break;
        default:
            throw new IllegalArgumentException("Unknown kind: " + kind);
        }

        boolean result = count == 0;
        log.debug("Is {} '{}' unique? {}", kind, normalizedName, result ? "yes" : "no");
        return result;
    }
}
