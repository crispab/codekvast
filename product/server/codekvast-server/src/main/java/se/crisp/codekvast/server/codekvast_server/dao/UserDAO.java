package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.dao.DataAccessException;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.Application;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import java.util.Collection;
import java.util.Map;

/**
 * @author Olle Hallin
 */
public interface UserDAO {

    long getCustomerId(String customerName) throws UndefinedCustomerException;

    /**
     * Retrieve an application ID. If not found, a new row is inserted into APPLICATIONS and an ApplicationCreatedEvent is posted on the
     * event bus.
     */
    long getAppId(long customerId, String environment, String appName, String appVersion) throws UndefinedApplicationException;

    AppId getAppIdByJvmFingerprint(String jvmFingerprint);

    int countUsersByUsername(String username);

    int countUsersByEmailAddress(String emailAddress);

    int countCustomersByNameLc(String customerName);

    long createUser(String fullName, String username, String emailAddress, String plaintextPassword, Role... roles)
            throws DataAccessException;

    void createCustomerWithPrimaryContact(String customerName, long userId) throws DataAccessException;

    Collection<InvocationEntry> getSignatures(Long customerId);

    /**
     * Retrieve all customers this user has access to.
     *
     * @param username The username
     * @return A map with customerId as keys and customerNames as values.
     */
    Map<Long, String> getCustomers(String username);

    /**
     * Retrieve all applications for a certain customer
     *
     * @param customerId The customerId
     * @return All applications for a certain customer. Does never return null.
     */
    Collection<Application> getApplications(Long customerId);
}
