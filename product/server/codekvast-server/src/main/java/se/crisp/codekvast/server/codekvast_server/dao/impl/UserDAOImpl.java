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
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.Application;
import se.crisp.codekvast.server.codekvast_server.model.Organisation;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * DAO for user, organisation and application data.
 *
 * @author olle.hallin@crisp.se
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
    public AppId getAppIdByJvmUuid(String jvmUuid) {
        log.debug("Looking up AppId for JVM {}...", jvmUuid);
        try {
            AppId result = jdbcTemplate
                    .queryForObject("SELECT id, organisation_id, application_id FROM jvm_info WHERE jvm_uuid = ?",
                                    new AppIdRowMapper(),
                                    jvmUuid);
            log.debug("Result = {}", result);
            return result;
        } catch (EmptyResultDataAccessException e) {
            log.info("No AppId found for JVM {}, probably an agent that uploaded stale data", jvmUuid);
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
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organisations WHERE LOWER(name) = ?", Integer.class, organisationName);
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
    public Set<SignatureEntry> getSignatures(long organisationId) {

        // The database contains several rows for the same signature, with different invoked_at values.
        // We only want to return the entries with highest (latest) invoked_at for each signature.
        //
        // The algorithm below relies on the fact that a java.util.Set.add() will not replace an already present element.
        // By ordering by invoked_at DESC, the first returned row (i.e., the latest invoked_at) will win.
        //
        // It is possible to do this as a one-liner because SignatureEntry.hashCode() and equals() uses SignatureEntry.signature only.
        //
        // PS. Doing the filtering in Java is magnitudes faster than trying to to the same in pure SQL.

        long startedAt = System.currentTimeMillis();
        Set<SignatureEntry> result = new HashSet<>(jdbcTemplate.query(
                "SELECT signature, invoked_at, confidence FROM signatures WHERE organisation_id = ? ORDER BY signature, invoked_at DESC",
                new SignatureEntryRowMapper(), organisationId));
        log.debug("getSignatures({}) took {} ms", organisationId, System.currentTimeMillis() - startedAt);
        return result;
    }

    private long doCreateOrganisation(String organisationName) {
        long organisationId = doInsertRow("INSERT INTO organisations(name) VALUES(?)", organisationName);
        Organisation organisation = new Organisation(organisationId, organisationName);
        log.info("Created {}", organisation);
        return organisationId;
    }

    private static class AppIdRowMapper implements RowMapper<AppId> {
        @Override
        public AppId mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AppId.builder()
                        .jvmId(rs.getLong("ID"))
                        .organisationId(rs.getLong("ORGANISATION_ID"))
                        .appId(rs.getLong("APPLICATION_ID"))
                        .build();
        }
    }

    private static class SignatureEntryRowMapper implements RowMapper<SignatureEntry> {
        @Override
        public SignatureEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            // SIGNATURE, INVOKED_AT, CONFIDENCE
            return new SignatureEntry(rs.getString(1), rs.getLong(2), SignatureConfidence.fromOrdinal(rs.getInt(3)));
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
