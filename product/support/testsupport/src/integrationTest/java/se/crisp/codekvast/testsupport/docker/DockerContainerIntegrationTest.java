package se.crisp.codekvast.testsupport.docker;

import org.junit.ClassRule;
import org.junit.Test;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
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
                                                .build())
            .build();

    @Test
    public void should_start_and_wait_for_mariadb() throws Exception {
        assumeTrue(mariadb.isRunning());
        assertThat(mariadb.getExternalPort(3306), not(is(0)));

        assertDataSourceIsReady(mariadb.getExternalPort(3306));
    }

    private void assertDataSourceIsReady(int port) throws IOException, SQLException {
        DataSource dataSource = new MariaDbDataSource("localhost", port, "somedatabase");
        try (Connection connection = dataSource.getConnection("nisse", "hult");
             Statement st = connection.createStatement()) {

            st.execute("SELECT 1 FROM DUAL");
        }
    }
}
