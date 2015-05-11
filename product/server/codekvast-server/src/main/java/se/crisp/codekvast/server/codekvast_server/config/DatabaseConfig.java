package se.crisp.codekvast.server.codekvast_server.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.crisp.codekvast.server.codekvast_server.migration.V1_1__DummyJavaMigration;
import se.crisp.codekvast.server.codekvast_server.util.DatabaseUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Initializes the database.
 * <p>
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
    public Flyway flyway(DataSource dataSource, CodekvastSettings codekvastSettings) throws SQLException {
        // Cannot use the jdbcTemplate bean, since it has not yet been constructed.
        // The method {@code @Bean jdbcTemplate(DataSource)} depends on the currently executing method.
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        boolean didRestore = false;
        if (!DatabaseUtils.isMemoryDatabase(jdbcTemplate)) {
            didRestore = DatabaseUtils.restoreDatabaseIfRestoreMeFileWasFound(jdbcTemplate, codekvastSettings);
        }

        log.info("Applying Flyway to {}", dataSource.getConnection().getMetaData().getURL());
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations(SQL_MIGRATION_LOCATION, JAVA_MIGRATION_LOCATION);

        MigrationInfo[] pendingMigrations = flyway.info().pending();
        if (pendingMigrations != null && pendingMigrations.length > 0) {

            if (!didRestore) {
                // Only backup if we did not restore from a backup...
                backupDatabaseBeforeMigration(jdbcTemplate, codekvastSettings, pendingMigrations);
            }
            logPendingMigrations(pendingMigrations);
        }

        flyway.migrate();

        replacePlaintextPasswords(dataSource.getConnection());

        return flyway;
    }

    private void logPendingMigrations(MigrationInfo[] pendingMigrations) {
        List<MigrationInfo> infos = Arrays.asList(pendingMigrations);
        String pending = infos.stream().map(mi -> String.format("%s %s", mi.getVersion(), mi.getDescription()))
                              .collect(Collectors.joining("\n    "));

        log.info("Will apply the following pending migrations:\n    {}", pending);
    }

    private void backupDatabaseBeforeMigration(JdbcTemplate jdbcTemplate, CodekvastSettings codekvastSettings,
                                               MigrationInfo[] pendingMigrations) throws SQLException {

        String firstPendingVersion = pendingMigrations[0].getVersion().toString();

        if (!DatabaseUtils.isMemoryDatabase(jdbcTemplate) && !firstPendingVersion.equals("1.0")) {
            long startedAt = System.currentTimeMillis();

            String firstPendingScript = pendingMigrations[0].getScript().replace(".sql", "").replace(".java", "");
            log.info("Backing up database before executing {}", firstPendingScript);

            String backupFile =
                    DatabaseUtils.getBackupFile(codekvastSettings, jdbcTemplate, new Date(), "before_" + firstPendingScript);
            DatabaseUtils.backupDatabase(jdbcTemplate, backupFile);

            log.info("Backed up database to {} in {} ms", backupFile, System.currentTimeMillis() - startedAt);
        }
    }

    private void replacePlaintextPasswords(Connection connection) throws SQLException {
        log.debug("Replacing plaintext passwords...");

        try (
                ResultSet resultSet = connection.createStatement().executeQuery(
                        "SELECT username, plaintext_password FROM users WHERE plaintext_password IS NOT NULL");
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
