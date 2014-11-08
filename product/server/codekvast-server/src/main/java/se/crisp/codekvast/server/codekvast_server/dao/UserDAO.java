package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.dao.DataAccessException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;
import se.crisp.codekvast.server.codekvast_server.model.Role;
import se.crisp.codekvast.server.codekvast_server.model.User;

/**
 * @author Olle Hallin
 */
public interface UserDAO {

    long getCustomerId(String customerName) throws UndefinedCustomerException;

    long getAppId(long customerId, String environment, String appName, String appVersion) throws UndefinedApplicationException;

    AppId getAppIdByJvmFingerprint(String jvmFingerprint);

    int countUsersByUsername(String username);

    int countUsersByEmailAddress(String emailAddress);

    int countCustomersByNameLc(String customerName);

    long createUser(User user, String plaintextPassword, Role... roles) throws DataAccessException;

    long createCustomerWithPrimaryContact(String customerName, long userId) throws DataAccessException, UndefinedCustomerException;

    @lombok.Value
    static class AppId {
        private final long customerId;
        private final long appId;
    }
}
