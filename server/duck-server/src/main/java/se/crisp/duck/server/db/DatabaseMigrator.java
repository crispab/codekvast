package se.crisp.duck.server.db;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @author Olle Hallin
 */
@Configuration
@Slf4j
public class DatabaseMigrator {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) throws SQLException {
        log.info("Migrating database at {}", dataSource.getConnection().getMetaData().getURL());
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocations(getClass().getPackage().getName() + ".migration");
        return flyway;
    }

}
