package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.controller.RegistrationController;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.exception.DuplicateNameException;
import se.crisp.codekvast.server.codekvast_server.model.Role;
import se.crisp.codekvast.server.codekvast_server.service.RegistrationService;

import javax.inject.Inject;
import java.util.Locale;

/**
 * @author Olle Hallin
 */
@Service
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private final UserDAO userDAO;

    @Inject
    public RegistrationServiceImpl(@NonNull UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    private String normalizeName(@NonNull String name) {
        return name.trim().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public boolean isUnique(@NonNull UniqueKind kind, @NonNull String name) {
        int count;
        String normalizedName = normalizeName(name);

        switch (kind) {
        case USERNAME:
            count = userDAO.countUsersByUsername(normalizedName);
            break;
        case EMAIL_ADDRESS:
            count = userDAO.countUsersByEmailAddress(normalizedName);
            break;
        case ORGANISATION_NAME:
            count = userDAO.countOrganisationsByNameLc(normalizedName);
            break;
        default:
            throw new IllegalArgumentException("Unknown kind: " + kind);
        }

        boolean result = count == 0;
        log.debug("Is {} '{}' unique? {}", kind.toString().toLowerCase().replace('_', ' '), normalizedName, result ? "yes" : "no");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long registerUserAndOrganisation(@NonNull RegistrationController.RegistrationRequest data) throws CodekvastException {
        try {
            long userId = userDAO.createUser(data.getFullName(), normalizeName(data.getUsername()), normalizeName(data.getEmailAddress()),
                                             data.getPassword(), Role.ADMIN, Role.USER);
            // TODO: create Role.AGENT
            userDAO.createOrganisationWithPrimaryContact(data.getOrganisationName(), userId);
            return userId;
        } catch (DuplicateKeyException e) {
            throw new DuplicateNameException("Cannot register " + data);
        } catch (DataAccessException e) {
            throw new CodekvastException("Cannot register " + data, e);
        }
    }
}
