package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;

/**
 * @author Olle Hallin
 */
public interface UserDAO {
    long getCustomerId(String customerName) throws UndefinedCustomerException;

    long getAppId(long customerId, String environment, String appName, String appVersion) throws UndefinedApplicationException;

    AppId getAppId(String jvmFingerprint);

    int countUsersByUsername(String username);

    int countCustomersByNameLc(String customerName);

    @lombok.Value
    static class AppId {
        private final long customerId;
        private final long appId;
    }
}
