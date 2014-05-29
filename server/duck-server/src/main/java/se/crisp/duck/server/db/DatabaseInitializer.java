package se.crisp.duck.server.db;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Initializes the database.
 *
 * <ol>
 * <li>runs Flyway.migrate().</li>
 * <li>creates a JdbcTemplate bean</li>
 *</ol>
 * @author Olle Hallin
 */
@Configuration
@Slf4j
public class DatabaseInitializer {
    public final String MIGRATION_LOCATION = getClass().getPackage().getName() + ".migration";

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) throws SQLException {
        log.info("Migrating database at {}", dataSource.getConnection().getMetaData().getURL());
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations(MIGRATION_LOCATION);
        return flyway;
    }

    /**
     * Override the default JdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first-
     */
    @Bean
    @DependsOn("flyway")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        log.info("Creates a JdbcTemplate");
        return new JdbcTemplate(dataSource);
    }

    /**
     * Override the default NamedParameterJdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first-
     */
    @Bean
    @DependsOn("flyway")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

}
