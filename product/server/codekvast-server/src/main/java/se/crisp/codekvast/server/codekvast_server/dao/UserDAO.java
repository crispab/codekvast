package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.dao.DataAccessException;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import java.util.Collection;

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

    long createUser(String fullName, String username, String emailAddress, String plaintextPassword, Role... roles)
            throws DataAccessException;

    long createCustomerWithPrimaryContact(String customerName, long userId) throws DataAccessException, UndefinedCustomerException;

    Collection<InvocationEntry> getSignatures(Long customerId) throws CodekvastException;

    @lombok.Value
    static class AppId {
        private final long customerId;
        private final long appId;
    }
}
