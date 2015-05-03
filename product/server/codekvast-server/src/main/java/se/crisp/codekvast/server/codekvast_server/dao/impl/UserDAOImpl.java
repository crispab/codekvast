package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;

import javax.inject.Inject;

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

}
