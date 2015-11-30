package se.crisp.codekvast.warehouse.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import se.crisp.codekvast.warehouse.migration.V1_0__DummyJavaMigration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final String JAVA_MIGRATION_LOCATION = V1_0__DummyJavaMigration.class.getPackage().getName();
    private static final String SQL_MIGRATION_LOCATION = "database.migration";

    @Bean
    public Flyway flyway(DataSource dataSource) throws SQLException {
        log.info("Applying Flyway to {}", dataSource.getConnection().getMetaData().getURL());
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations(SQL_MIGRATION_LOCATION, JAVA_MIGRATION_LOCATION);

        MigrationInfo[] pendingMigrations = flyway.info().pending();
        boolean werePendingMigrations = false;
        if (pendingMigrations != null && pendingMigrations.length > 0) {
            logPendingMigrations(pendingMigrations);
            werePendingMigrations = true;
        }

        long startedAt = System.currentTimeMillis();

        flyway.migrate();

        if (werePendingMigrations) {
            log.info("Database migrated in {} ms", System.currentTimeMillis() - startedAt);
        }
        return flyway;
    }

    private void logPendingMigrations(MigrationInfo[] pendingMigrations) {
        List<MigrationInfo> infos = Arrays.asList(pendingMigrations);
        String pending = infos.stream().map(mi -> String.format("%s %s", mi.getVersion(), mi.getDescription()))
                              .collect(Collectors.joining("\n    "));

        log.info("Will apply the following pending migrations:\n    {}", pending);
    }

    /*
     * Override the default JdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first.
     */
    @Bean
    @DependsOn("flyway")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws SQLException {
        log.debug("Create a JdbcTemplate");
        return new JdbcTemplate(dataSource);
    }

    /*
     * Override the default NamedParameterJdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first.
     */
    @Bean
    @DependsOn("flyway")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        log.debug("Creates a NamedParameterJdbcTemplate");
        return new NamedParameterJdbcTemplate(dataSource);
    }


}
