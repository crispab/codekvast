package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;
import se.crisp.codekvast.server.codekvast_server.model.Role;
import se.crisp.codekvast.server.codekvast_server.model.User;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkState;

/**
 * DAO for user and application data.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class UserDAOImpl implements UserDAO {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final Boolean autoCreateCustomer;

    @Inject
    public UserDAOImpl(JdbcTemplate jdbcTemplate,
                       PasswordEncoder passwordEncoder,
                       @Value("${codekvast.auto-register-customer}") Boolean autoCreateCustomer) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.autoCreateCustomer = autoCreateCustomer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Cacheable("user")
    public long getCustomerId(final String customerName) throws UndefinedCustomerException {
        log.debug("Looking up customer id for '{}'", customerName);
        return doGetOrCreateCustomer(customerName, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Cacheable("user")
    public long getAppId(long customerId, String environment, String appName, String appVersion) throws UndefinedApplicationException {
        log.debug("Looking up app id for {}:{}:{}", customerId, appName, appVersion);
        return doGetOrCreateApp(customerId, environment, appName, appVersion, true);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("user")
    public AppId getAppId(String jvmFingerprint) {
        log.debug("Looking up AppId for JVM {}...", jvmFingerprint);
        AppId result = jdbcTemplate
                .queryForObject("SELECT CUSTOMER_ID, APPLICATION_ID FROM JVM_RUNS WHERE JVM_FINGERPRINT = ?", new AppIdRowMapper(),
                                jvmFingerprint);
        log.debug("Result = {}", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public int countUsersByUsername(@NonNull String username) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM USERS WHERE USERNAME = ?", Integer.class, username);
    }

    @Override
    @Transactional(readOnly = true)
    public int countCustomersByNameLc(@NonNull String customerName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CUSTOMERS WHERE NAMELC = ?", Integer.class, customerName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createUser(User user, String plaintextPassword, Role... roles) {
        jdbcTemplate.update("INSERT INTO USERS(FULL_NAME, EMAIL, USERNAME, ENCODED_PASSWORD) VALUES(?, ?, ?, ?)", user
                                    .getFullName(),
                            user.getEmailAddress(), user.getUsername(), passwordEncoder.encode(plaintextPassword));
        long userId = jdbcTemplate.queryForObject("SELECT IDENTITY()", Long.class);
        log.info("Created user {} with id {}", user, userId);

        for (Role role : roles) {
            jdbcTemplate.update("INSERT INTO USER_ROLES(USER_ID, ROLE) VALUES (?, ?)", userId, role.name());
            log.info("Assigned role {} to {}", role, user.getUsername());
        }

        return userId;
    }

    @Override
    public long createCustomerWithMember(String customerName, long userId) throws UndefinedCustomerException {
        long customerId = doGetOrCreateCustomer(customerName, false);
        jdbcTemplate.update("INSERT INTO CUSTOMER_MEMBERS(CUSTOMER_ID, USER_ID, PRIMARY_CONTACT) VALUES(?, ?, ?)", customerId, userId,
                            true);
        return customerId;
    }

    @Override
    public long createApplication(long customerId, String appName) {
        jdbcTemplate.update("INSERT INTO APPLICATIONS(CUSTOMER_ID, NAME) VALUES (?, ?)", customerId, appName);
        Long appId = jdbcTemplate.queryForObject("SELECT IDENTITY()", Long.class);
        log.info("Created application '{}' for customer {} with id {}", appName, customerId, appId);
        return appId;
    }

    private long doGetOrCreateCustomer(final String customerName, boolean selectFirst) throws UndefinedCustomerException {
        if (selectFirst) {
            try {
                return jdbcTemplate.queryForObject("SELECT id FROM customers WHERE name = ?", Long.class, customerName);
            } catch (EmptyResultDataAccessException ignored) {
            }
            if (!autoCreateCustomer) {
                throw new UndefinedCustomerException("No such customer: " + customerName);
            }
        }
        jdbcTemplate.update("INSERT INTO customers(name) VALUES(?)", customerName);
        long customerId = jdbcTemplate.queryForObject("SELECT IDENTITY()", Long.class);
        log.info("Created customer '{}' with id {}", customerName, customerId);
        return customerId;
    }

    private Long doGetOrCreateApp(long customerId, String environment, String appName, String appVersion, boolean allowRecursion)
            throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM applications " +
                                                       "WHERE customer_id = ? AND environment = ? AND name = ? AND version = ? ",
                                               Long.class,
                                               customerId, environment, appName, appVersion);
        } catch (EmptyResultDataAccessException ignored) {
        }

        checkState(allowRecursion, "Endless recursion detected");

        int updated = jdbcTemplate.update("INSERT INTO applications(customer_id, environment, name, version) VALUES(?, ?, ?, ?)",
                                          customerId, environment, appName, appVersion);
        if (updated > 0) {
            log.info("Created application {}:{}:{}:{}", customerId, environment,
                     appName, appVersion);
            return doGetOrCreateApp(customerId, environment, appName, appVersion, false);
        }

        throw new IllegalStateException("Could not insert application");
    }

    private static class AppIdRowMapper implements RowMapper<AppId> {
        @Override
        public AppId mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return new AppId(rs.getLong(1), rs.getLong(2));
        }
    }

}
