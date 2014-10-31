package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.cache.annotation.Cacheable;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;

/**
 * @author Olle Hallin
 */
public interface UserDAO {
    long getCustomerId(String customerName) throws UndefinedCustomerException;

    long getAppId(long customerId, String environment, String appName, String appVersion) throws UndefinedApplicationException;

    @Cacheable("application")
    AppId getAppId(String jvmFingerprint);

    @lombok.Value
    static class AppId {
        private final long customerId;
        private final long appId;
    }
}
