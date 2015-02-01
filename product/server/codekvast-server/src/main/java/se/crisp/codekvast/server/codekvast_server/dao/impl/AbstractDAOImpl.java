package se.crisp.codekvast.server.codekvast_server.dao.impl;

import com.google.common.eventbus.EventBus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base class for DAO implementations.
 *
 * @author Olle Hallin <olle.hallin@crisp.se>
 */
public abstract class AbstractDAOImpl {

    protected final JdbcTemplate jdbcTemplate;
    protected final EventBus eventBus;

    public AbstractDAOImpl(EventBus eventBus,
                           JdbcTemplate jdbcTemplate) {
        this.eventBus = eventBus;
        this.jdbcTemplate = jdbcTemplate;
    }

    protected long doInsertRow(String sql, Object... args) {
        checkArgument(sql.toUpperCase().startsWith("INSERT INTO "));
        jdbcTemplate.update(sql, args);
        return jdbcTemplate.queryForObject("SELECT IDENTITY()", Long.class);
    }

    protected Collection<String> getUsernamesInOrganisation(long organisationId) {
        return jdbcTemplate.queryForList("SELECT u.username " +
                                                 "FROM users u, organisation_members m " +
                                                 "WHERE u.id = m.user_id AND m.organisation_id = ?",
                                         String.class, organisationId);
    }
}
