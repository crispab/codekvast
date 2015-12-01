/**
 * Copyright (c) 2015 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    public String dataSourceReady(DataSource dataSource) throws SQLException {
        int errorCount = 0;
        while (errorCount < 6) {
            try (Statement statement = dataSource.getConnection().createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                if (errorCount > 0) {
                    log.info("Finally connected to database after {} failed attempts", errorCount);
                }
                return getDataSourceUrl(dataSource);
            } catch (SQLException e) {
                errorCount += 1;
                log.warn("Cannot connect to database, sleeping 10s before trying again...");
                sleepSeconds(10);
            }
        }
        throw new SQLException("Could not connect to database");
    }

    private void sleepSeconds(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException ignore) {

        }
    }

    private String getDataSourceUrl(DataSource dataSource) throws SQLException {
        return dataSource.getConnection().getMetaData().getURL();
    }

    @Bean
    @DependsOn("dataSourceReady")
    public Flyway flyway(DataSource dataSource) throws SQLException {
        log.info("Applying Flyway to {}", getDataSourceUrl(dataSource));
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
