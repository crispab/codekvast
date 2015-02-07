package se.crisp.codekvast.server.codekvast_server.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.crisp.codekvast.server.codekvast_server.migration.V1_1__DummyJavaMigration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Initializes the database.
 *
 * <ol> <li>runs Flyway.migrate().</li> <li>creates a JdbcTemplate bean</li> </ol>
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
@Slf4j
public class DatabaseConfig {
    public static final String JAVA_MIGRATION_LOCATION = V1_1__DummyJavaMigration.class.getPackage().getName();
    public static final String SQL_MIGRATION_LOCATION = "database.migration";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Flyway flyway(DataSource dataSource) throws SQLException {
        // TODO: perform a backup before migrating database
        log.info("Migrating database at {}", dataSource.getConnection().getMetaData().getURL());
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations(SQL_MIGRATION_LOCATION, JAVA_MIGRATION_LOCATION);
        flyway.migrate();

        replacePlaintextPasswords(dataSource.getConnection());

        return flyway;
    }

    private void replacePlaintextPasswords(Connection connection) throws SQLException {
        log.debug("Replacing plaintext passwords...");

        try (
                ResultSet resultSet = connection.createStatement()
                                                .executeQuery(
                                                        "SELECT username, plaintext_password FROM users WHERE plaintext_password IS NOT " +
                                                                "NULL");
                PreparedStatement update = connection
                        .prepareStatement("UPDATE users SET encoded_password = ?, plaintext_password = NULL WHERE username = ?")) {

            PasswordEncoder passwordEncoder = passwordEncoder();

            while (resultSet.next()) {
                String username = resultSet.getString(1);
                String plaintextPassword = resultSet.getString(2);

                update.setString(1, passwordEncoder.encode(plaintextPassword));
                update.setString(2, username);
                int updated = update.executeUpdate();
                if (updated == 0) {
                    log.error("Could not encode password for '{}': not found", username);
                } else {
                    log.info("Encoded password for '{}'", username);
                }
            }
        }

    }

    /**
     * Override the default JdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first.
     */
    @Bean
    @DependsOn("flyway")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws SQLException {
        log.debug("Create a JdbcTemplate");
        return new JdbcTemplate(dataSource);
    }

    /**
     * Override the default NamedParameterJdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first.
     */
    @Bean
    @DependsOn("flyway")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        log.debug("Creates a NamedParameterJdbcTemplate");
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
