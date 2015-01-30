package se.crisp.codekvast.server.codekvast_server.dao;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.server.codekvast_server.config.DatabaseConfig;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

/**
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceAutoConfiguration.class, DatabaseConfig.class})
@IntegrationTest({
        "spring.datasource.url=jdbc:mysql://localhost/codekvast_integrationTest",
        "spring.datasource.username=root",
        "spring.datasource.password=root",
})
@Ignore("WIP: porting from H2 to MySQL")
public class FlywayTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testFlywayMigrations() {
        List<String> usernames = jdbcTemplate.queryForList("SELECT username FROM USERS ORDER BY username", String.class);
        assertThat(usernames, contains("admin", "agent", "monitor", "system", "user"));
    }
}
