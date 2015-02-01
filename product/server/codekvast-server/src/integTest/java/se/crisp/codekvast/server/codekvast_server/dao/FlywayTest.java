package se.crisp.codekvast.server.codekvast_server.dao;

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
 * @author Olle Hallin <olle.hallin@crisp.se>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceAutoConfiguration.class, DatabaseConfig.class})
@IntegrationTest({
        "spring.datasource.url=jdbc:h2:mem:daoTest",
})
public class FlywayTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testFlywayMigrations() {
        List<String> usernames = jdbcTemplate.queryForList("SELECT username FROM USERS ORDER BY username", String.class);
        assertThat(usernames, contains("admin", "agent", "monitor", "system", "user"));
    }
}
