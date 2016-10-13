package se.crisp.codekvast.testsupport.docker;

import org.junit.ClassRule;
import org.junit.Test;
import org.mariadb.jdbc.MariaDbDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assume.assumeTrue;

/**
 * @author olle.hallin@crisp.se
 */
public class DockerContainerIntegrationTest {

    @ClassRule
    public static DockerContainer mariadb = DockerContainer
            .builder()
            .imageName("mariadb:10")
            .port("3306")

            .env("MYSQL_ROOT_PASSWORD=foobar")
            .env("MYSQL_DATABASE=somedatabase")
            .env("MYSQL_USER=nisse")
            .env("MYSQL_PASSWORD=hult")

            .readyChecker(
                    MariaDbContainerReadyChecker.builder()
                                                .host("localhost")
                                                .internalPort(3306)
                                                .database("somedatabase")
                                                .username("nisse")
                                                .password("hult")
                                                .timeoutSeconds(120)
                                                .assignJdbcUrlToSystemProperty("my.jdbcUrl")
                                                .build())
            .build();

    @Test
    public void should_start_and_wait_for_mariadb() throws Exception {
        // given
        // class rule has started MariaDB in a Docker container

        // when
        assumeTrue(mariadb.isRunning());

        // then
        assertDataSourceIsReady(System.getProperty("my.jdbcUrl"));
    }

    private void assertDataSourceIsReady(String jdbcUrl) throws IOException, SQLException {
        MariaDbDataSource dataSource = new MariaDbDataSource(jdbcUrl);
        try (Connection connection = dataSource.getConnection("nisse", "hult");
             Statement st = connection.createStatement()) {

            st.execute("SELECT 1 FROM DUAL");
        }
    }
}
