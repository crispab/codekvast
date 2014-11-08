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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * DAO for user, customer and application data.
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
        try {
            return jdbcTemplate.queryForObject("SELECT ID FROM CUSTOMERS WHERE NAME = ?", Long.class, customerName);
        } catch (EmptyResultDataAccessException ignored) {
        }
        if (!autoCreateCustomer) {
            throw new UndefinedCustomerException("No such customer: " + customerName);
        }
        return doCreateCustomer(customerName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Cacheable("user")
    public long getAppId(long customerId, String environment, String appName, String appVersion) throws UndefinedApplicationException {
        log.debug("Looking up app id for {}:{}:{}", customerId, appName, appVersion);
        return doGetOrCreateApp(customerId, environment, appName, appVersion);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("user")
    public AppId getAppIdByJvmFingerprint(String jvmFingerprint) {
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
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CUSTOMERS WHERE NAME_LC = ?", Integer.class, customerName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createUser(User user, String plaintextPassword, Role... roles) {
        long userId = doInsertRow("INSERT INTO USERS(FULL_NAME, EMAIL, USERNAME, ENCODED_PASSWORD) VALUES(?, ?, ?, ?)", user
                                          .getFullName(),
                                  user.getEmailAddress(), user.getUsername(), passwordEncoder.encode(plaintextPassword));
        log.info("Created user {}:{}", userId, user);

        for (Role role : roles) {
            jdbcTemplate.update("INSERT INTO USER_ROLES(USER_ID, ROLE) VALUES (?, ?)", userId, role.name());
            log.info("Assigned role {} to {}", role, user.getUsername());
        }

        return userId;
    }

    @Override
    public long createCustomerWithPrimaryContact(String customerName, long userId) throws UndefinedCustomerException {
        long customerId = doCreateCustomer(customerName);
        jdbcTemplate.update("INSERT INTO CUSTOMER_MEMBERS(CUSTOMER_ID, USER_ID, PRIMARY_CONTACT) VALUES(?, ?, ?)", customerId, userId,
                            true);
        return customerId;
    }

    private long doCreateCustomer(String customerName) {
        long customerId = doInsertRow("INSERT INTO customers(name) VALUES(?)", customerName);
        log.info("Created customer {}:'{}'", customerId, customerName);
        return customerId;
    }

    private Long doGetOrCreateApp(long customerId, String environment, String appName, String appVersion)
            throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT ID FROM APPLICATIONS " +
                                                       "WHERE CUSTOMER_ID = ? AND ENVIRONMENT = ? AND NAME = ? AND VERSION = ? ",
                                               Long.class, customerId, environment, appName, appVersion);
        } catch (EmptyResultDataAccessException ignored) {
        }

        long appId = doInsertRow("INSERT INTO applications(customer_id, environment, name, version) VALUES(?, ?, ?, ?)",
                                 customerId, environment, appName, appVersion);
        log.info("Created application {}:{}:'{}':'{}':'{}'", customerId, appId, environment, appName, appVersion);
        return appId;
    }

    private long doInsertRow(String sql, Object... args) {
        checkArgument(sql.toUpperCase().startsWith("INSERT INTO "));
        jdbcTemplate.update(sql, args);
        return jdbcTemplate.queryForObject("SELECT IDENTITY()", Long.class);
    }

    private static class AppIdRowMapper implements RowMapper<AppId> {
        @Override
        public AppId mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return new AppId(rs.getLong(1), rs.getLong(2));
        }
    }

}
