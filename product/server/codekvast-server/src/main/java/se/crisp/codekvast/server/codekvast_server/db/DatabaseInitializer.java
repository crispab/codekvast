package se.crisp.codekvast.server.codekvast_server.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/*
 No JDBC support for now
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
*/

/**
 * Initializes the database.
 * <p/>
 * <ol> <li>runs Flyway.migrate().</li> <li>creates a JdbcTemplate bean</li> </ol>
 *
 * @author Olle Hallin
 */
@Configuration
@Slf4j
public class DatabaseInitializer {
    public final String MIGRATION_LOCATION = getClass().getPackage().getName() + ".migration";

    /*
    * Disabled for now
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) throws SQLException {
        log.info("Migrating database at {}", dataSource.getConnection().getMetaData().getURL());
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations(MIGRATION_LOCATION);
        return flyway;
    }
    */

    /**
     * Override the default JdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first-
     @Bean
     @DependsOn("flyway")
     public JdbcTemplate jdbcTemplate(DataSource dataSource) {
     log.debug("Creates a JdbcTemplate");
     return new JdbcTemplate(dataSource);
     }
     */

    /**
     * Override the default NamedParameterJdbcTemplate created by Spring Boot, to make sure that Flyway.migrate() has run first-
     @Bean
     @DependsOn("flyway")
     public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
     log.debug("Creates a NamedParameterJdbcTemplate");
     return new NamedParameterJdbcTemplate(dataSource);
     }
     */
}
