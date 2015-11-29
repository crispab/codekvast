package se.crisp.codekvast.server.codekvast_server.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;

/**
 * This is a dummy Java migration just to keep Flyway happy.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("UnusedDeclaration")
public class V1_1__DummyJavaMigration implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {
        // NOTHING HERE!
    }
}
