package se.crisp.codekvast.server.codekvast_server.dao.impl;

import com.google.common.eventbus.EventBus;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.ApplicationCreatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.Application;
import se.crisp.codekvast.server.codekvast_server.model.Customer;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

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
    private final EventBus eventBus;

    @Inject
    public UserDAOImpl(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, EventBus eventBus) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Cacheable("user")
    public long usernameToCustomerId(final String username) throws UndefinedCustomerException {
        log.debug("Looking up customer id for username '{}'", username);
        try {
            return jdbcTemplate.queryForObject("SELECT cm.CUSTOMER_ID FROM CUSTOMER_MEMBERS cm, USERS u " +
                                                       "WHERE cm.USER_ID = u.ID " +
                                                       "AND u.USERNAME = ?", Long.class, username);
        } catch (EmptyResultDataAccessException ignored) {
        }
        throw new UndefinedCustomerException("No such user: " + username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Cacheable("user")
    public long getAppId(long customerId, String appName) throws UndefinedApplicationException {
        log.debug("Looking up app id for {}:{}", customerId, appName);
        return doGetOrCreateApp(customerId, appName);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("user")
    public AppId getAppIdByJvmFingerprint(String jvmFingerprint) {
        log.debug("Looking up AppId for JVM {}...", jvmFingerprint);
        try {
            AppId result = jdbcTemplate
                    .queryForObject("SELECT CUSTOMER_ID, APPLICATION_ID FROM JVM_RUNS WHERE JVM_FINGERPRINT = ?", new AppIdRowMapper(),
                                    jvmFingerprint);
            log.debug("Result = {}", result);
            return result;
        } catch (EmptyResultDataAccessException e) {
            log.info("No AppId found for JVM {}, probably an agent that uploaded stale data", jvmFingerprint);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countUsersByUsername(@NonNull String username) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM USERS WHERE USERNAME = ?", Integer.class, username);
    }

    @Override
    @Transactional(readOnly = true)
    public int countUsersByEmailAddress(@NonNull String emailAddress) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM USERS WHERE EMAIL_ADDRESS = ?", Integer.class, emailAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public int countCustomersByNameLc(@NonNull String customerName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CUSTOMERS WHERE NAME_LC = ?", Integer.class, customerName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createUser(String fullName, String username, String emailAddress, String plaintextPassword, Role... roles) {
        long userId = doInsertRow("INSERT INTO USERS(FULL_NAME, USERNAME, EMAIL_ADDRESS, ENCODED_PASSWORD) VALUES(?, ?, ?, ?)",
                                  fullName, username, emailAddress, passwordEncoder.encode(plaintextPassword));
        log.info("Created user {}:'{}':'{}':'{}'", userId, fullName, username, emailAddress);

        for (Role role : roles) {
            jdbcTemplate.update("INSERT INTO USER_ROLES(USER_ID, ROLE) VALUES (?, ?)", userId, role.name());
            log.info("Assigned role {} to {}:'{}'", role, userId, username);
        }

        return userId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCustomerWithPrimaryContact(String customerName, long userId) {
        long customerId = doCreateCustomer(customerName);
        jdbcTemplate.update("INSERT INTO CUSTOMER_MEMBERS(CUSTOMER_ID, USER_ID, PRIMARY_CONTACT) VALUES(?, ?, ?)", customerId, userId,
                            true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InvocationEntry> getSignatures(String username) {
        // TODO: include username in query
        return jdbcTemplate.query("SELECT SIGNATURE, INVOKED_AT, CONFIDENCE FROM SIGNATURES ", new InvocationsEntryRowMapper());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("user")
    public Collection<Application> getApplications(String username) {
        // TODO: include username in query
        return jdbcTemplate.query("SELECT ID, CUSTOMER_ID, NAME FROM APPLICATIONS ", new ApplicationRowMapper());
    }

    private long doCreateCustomer(String customerName) {
        long customerId = doInsertRow("INSERT INTO customers(name) VALUES(?)", customerName);
        Customer customer = Customer.builder().id(customerId).name(customerName).build();
        log.info("Created {}", customer);
        return customerId;
    }

    private Long doGetOrCreateApp(long customerId, String appName)
            throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT ID FROM APPLICATIONS " +
                                                       "WHERE CUSTOMER_ID = ? AND NAME = ? ",
                                               Long.class, customerId, appName);
        } catch (EmptyResultDataAccessException ignored) {
        }

        long appId = doInsertRow("INSERT INTO applications(customer_id, name) VALUES(?, ?)", customerId, appName);

        Application app = Application.builder()
                                     .appId(AppId.builder().customerId(customerId).appId(appId).build())
                                     .name(appName)
                                     .build();
        eventBus.post(new ApplicationCreatedEvent(app, Arrays.asList("user", "system")));

        log.info("Created {}", app);
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
            return AppId.builder()
                        .customerId(rs.getLong("CUSTOMER_ID"))
                        .appId(rs.getLong("APPLICATION_ID"))
                        .build();
        }
    }

    private static class InvocationsEntryRowMapper implements RowMapper<InvocationEntry> {
        public static final Long EPOCH = 0L;

        @Override
        public InvocationEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new InvocationEntry(rs.getString(1), getTimeMillis(rs, 2), SignatureConfidence.fromOrdinal(rs.getInt(3)));
        }

        private Long getTimeMillis(ResultSet rs, int columnIndex) throws SQLException {
            Date date = rs.getTimestamp(columnIndex);
            return date == null ? EPOCH : Long.valueOf(date.getTime());
        }
    }

    private class ApplicationRowMapper implements RowMapper<Application> {
        @Override
        public Application mapRow(ResultSet rs, int rowNum) throws SQLException {
            // ID, CUSTOMER_ID, NAME, VERSION
            return Application.builder()
                              .appId(AppId.builder()
                                          .appId(rs.getLong("ID"))
                                          .customerId(rs.getLong("CUSTOMER_ID"))
                                          .build())
                              .name(rs.getString("NAME"))
                              .build();
        }
    }


}
