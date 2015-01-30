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
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.Application;
import se.crisp.codekvast.server.codekvast_server.model.Organisation;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * DAO for user, organisation and application data.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class UserDAOImpl extends AbstractDAOImpl implements UserDAO {

    private final PasswordEncoder passwordEncoder;

    @Inject
    public UserDAOImpl(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, EventBus eventBus) {
        super(eventBus, jdbcTemplate);
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Cacheable("user")
    public long getOrganisationIdForUsername(final String username) throws UndefinedUserException {
        log.debug("Looking up organisation id for username '{}'", username);
        try {
            return jdbcTemplate.queryForObject("SELECT cm.organisation_id FROM organisation_members cm, users u " +
                                                       "WHERE cm.user_id = u.id " +
                                                       "AND u.username = ?", Long.class, username);
        } catch (EmptyResultDataAccessException ignored) {
            throw new UndefinedUserException("No such user: '" + username + "'");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("user")
    public AppId getAppIdByJvmFingerprint(String jvmFingerprint) {
        log.debug("Looking up AppId for JVM {}...", jvmFingerprint);
        try {
            AppId result = jdbcTemplate
                    .queryForObject("SELECT organisation_id, application_id FROM jvm_runs WHERE jvm_fingerprint = ?", new AppIdRowMapper(),
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
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
    }

    @Override
    @Transactional(readOnly = true)
    public int countUsersByEmailAddress(@NonNull String emailAddress) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email_address = ?", Integer.class, emailAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public int countOrganisationsByNameLc(@NonNull String organisationName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organisations WHERE name_lc = ?", Integer.class, organisationName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createUser(String fullName, String username, String emailAddress, String plaintextPassword, Role... roles) {
        long userId = doInsertRow("INSERT INTO USERS(FULL_NAME, USERNAME, EMAIL_ADDRESS, ENCODED_PASSWORD) VALUES(?, ?, ?, ?)",
                                  fullName, username, emailAddress, passwordEncoder.encode(plaintextPassword));
        log.info("Created user {}:'{}':'{}':'{}'", userId, fullName, username, emailAddress);

        for (Role role : roles) {
            jdbcTemplate.update("INSERT INTO user_roles(user_id, role) VALUES (?, ?)", userId, role.name());
            log.info("Assigned role {} to {}:'{}'", role, userId, username);
        }

        return userId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrganisationWithPrimaryContact(String organisationName, long userId) {
        long organisationId = doCreateOrganisation(organisationName);
        jdbcTemplate.update("INSERT INTO organisation_members(organisation_id, user_id, primary_contact) VALUES(?, ?, ?)", organisationId,
                            userId,
                            true);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InvocationEntry> getSignatures(long organisationId) {
        return jdbcTemplate.query("SELECT signature, invoked_at, confidence FROM signatures " +
                                          "WHERE organisation_id = ?", new InvocationsEntryRowMapper(), organisationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Application> getApplications(long organisationId) {
        return jdbcTemplate.query("SELECT id, organisation_id, name FROM applications " +
                                          "WHERE organisation_id = ?", new ApplicationRowMapper(), organisationId);
    }

    private long doCreateOrganisation(String organisationName) {
        long organisationId = doInsertRow("INSERT INTO organisations(name) VALUES(?)", organisationName);
        Organisation organisation = new Organisation(organisationId, organisationName);
        log.info("Created {}", organisation);
        return organisationId;
    }

    private static class AppIdRowMapper implements RowMapper<AppId> {
        @Override
        public AppId mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return AppId.builder()
                        .organisationId(rs.getLong("ORGANISATION_ID"))
                        .appId(rs.getLong("APPLICATION_ID"))
                        .build();
        }
    }

    private static class InvocationsEntryRowMapper implements RowMapper<InvocationEntry> {
        @Override
        public InvocationEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            // SIGNATURE, INVOKED_AT, CONFIDENCE
            return new InvocationEntry(rs.getString(1), rs.getLong(2), SignatureConfidence.fromOrdinal(rs.getInt(3)));
        }
    }

    private class ApplicationRowMapper implements RowMapper<Application> {
        @Override
        public Application mapRow(ResultSet rs, int rowNum) throws SQLException {
            // ID, ORGANISATION_ID, NAME
            return new Application(AppId.builder().appId(rs.getLong("ID")).organisationId(rs.getLong("ORGANISATION_ID")).build(),
                                   rs.getString("NAME"));
        }
    }
}
