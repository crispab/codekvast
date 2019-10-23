package io.codekvast.testsupport.docker;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.mariadb.jdbc.MariaDbDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertTrue;

/**
 * @author olle.hallin@crisp.se
 */
public class DockerContainerIntegrationTest {

    @ClassRule
    public static DockerContainer mariadb = DockerContainer.builder()
                                                           .imageName("mariadb:10.4")
                                                           .port("3306")

                                                           .env("MYSQL_ROOT_PASSWORD=root")
                                                           .env("MYSQL_DATABASE=somedatabase")
                                                           .env("MYSQL_USER=nisse")
                                                           .env("MYSQL_PASSWORD=hult")
                                                           .env("MYSQL_INITDB_SKIP_TZINFO=true")

                                                           .readyChecker(
                                                               MariaDbContainerReadyChecker.builder()
                                                                                           .hostname("localhost")
                                                                                           .internalPort(3306)
                                                                                           .database("somedatabase")
                                                                                           .username("nisse")
                                                                                           .password("hult")
                                                                                           .timeoutSeconds(30)
                                                                                           .assignJdbcUrlToSystemProperty("my.jdbcUrl")
                                                                                           .build())
                                                           .build();

    @ClassRule
    public static DockerContainer rabbitmq = DockerContainer.builder()
                                                            .imageName("rabbitmq:3.8-management-alpine")
                                                            .port("5672")

                                                            .env("RABBITMQ_DEFAULT_VHOST=some-vhost")
                                                            .env("RABBITMQ_DEFAULT_USER=nisse")
                                                            .env("RABBITMQ_DEFAULT_PASS=hult")

                                                            .readyChecker(
                                                                RabbitmqContainerReadyChecker.builder()
                                                                                             .host("localhost")
                                                                                             .internalPort(5672)
                                                                                             .vhost("some-vhost")
                                                                                             .timeoutSeconds(30)
                                                                                             .username("nisse")
                                                                                             .password("hult")
                                                                                             .assignRabbitUrlToSystemProperty("my.rabbitUrl")
                                                                                             .build())
                                                            .build();

    @Test
    public void should_start_and_wait_for_mariadb() throws Exception {
        // given
        // class rule has started MariaDB in a Docker container

        // when
        assertTrue(mariadb.isRunning());

        // then
        assertDataSourceIsReady(System.getProperty("my.jdbcUrl"));
    }

    @Test
    public void should_start_and_wait_for_rabbitmq() throws Exception {
        // given
        // class rule has started RabbitMQ in a Docker container

        // when
        assertTrue(rabbitmq.isRunning());

        // then
        assertAmqpIsReady(System.getProperty("my.rabbitUrl"));
    }

    private void assertAmqpIsReady(String amqpUrl) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(amqpUrl);
        com.rabbitmq.client.Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();
        channel.exchangeDeclare("exchangeName", "direct", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, "exchangeName", "routingKey");
        channel.close();
        conn.close();
    }

    private void assertDataSourceIsReady(String jdbcUrl) throws SQLException {
        MariaDbDataSource dataSource = new MariaDbDataSource(jdbcUrl);
        try (Connection connection = dataSource.getConnection("nisse", "hult");
             Statement st = connection.createStatement()) {

            st.execute("SELECT 1 FROM DUAL");
        }
    }
}
