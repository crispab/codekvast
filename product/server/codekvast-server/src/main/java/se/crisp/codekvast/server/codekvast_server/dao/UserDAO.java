package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.dao.DataAccessException;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.Application;
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

    void createCustomerWithPrimaryContact(String customerName, long userId) throws DataAccessException;

    Collection<InvocationEntry> getSignatures(Long customerId);

    Collection<Long> getCustomerIds(String username);

    Collection<Application> getApplications(Long customerId);
}
