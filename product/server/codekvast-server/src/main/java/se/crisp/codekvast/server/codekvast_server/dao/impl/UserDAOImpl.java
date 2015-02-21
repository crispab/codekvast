package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDisplay;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * DAO for user stuff.
 *
 * @author olle.hallin@crisp.se
 */
@Repository
@Slf4j
public class UserDAOImpl extends AbstractDAOImpl implements UserDAO {

    @Inject
    public UserDAOImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
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
    public Set<SignatureDisplay> getSignatures(long organisationId) {

        // The database contains several rows for the same signature, with different invoked_at_millis values.
        // We only want to return the entries with highest (latest) invoked_at_millis for each signature.
        //
        // The algorithm below relies on the fact that a java.util.Set.add() will not replace an already present element.
        // By ordering by invoked_at_millis DESC, the first returned row (i.e., the latest invoked_at_millis) will win.
        //
        // It is possible to do this as a one-liner because SignatureDisplay.hashCode() and equals() uses SignatureDisplay.name only.
        //
        // PS. Doing the filtering in Java is magnitudes faster than trying to to the same in pure SQL.

        long startedAt = System.currentTimeMillis();
        Set<SignatureDisplay> result = new HashSet<>(jdbcTemplate.query(
                "SELECT signature, invoked_at_millis, millis_since_jvm_start FROM signatures WHERE organisation_id = ? " +
                        "ORDER BY signature, invoked_at_millis DESC ",
                new SignatureDisplayRowMapper(), organisationId));
        log.debug("getSignatures({}) took {} ms", organisationId, System.currentTimeMillis() - startedAt);
        return result;
    }

    private static class SignatureDisplayRowMapper implements RowMapper<SignatureDisplay> {
        @Override
        public SignatureDisplay mapRow(ResultSet rs, int rowNum) throws SQLException {
            return SignatureDisplay.builder()
                                   .name(rs.getString(1))
                                   .invokedAtMillis(rs.getLong(2))
                                   .millisSinceJvmStart(rs.getLong(3))
                                   .build();
        }
    }

}
