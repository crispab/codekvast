package integrationTest.backoffice

import io.codekvast.backoffice.CodekvastBackofficeApplication
import io.codekvast.backoffice.service.MailSender
import io.codekvast.backoffice.weeding.WeedingTask
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Assume.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.inject.Inject

/** @author olle.hallin@crisp.se
 */
@SpringBootTest(
    classes = [CodekvastBackofficeApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("integrationTest", "dev-secrets", "no-mail-sender")
@Transactional
@Testcontainers
class BackofficeIntegrationTest {

    class KMariaDBContainer(private val image: String) : MariaDBContainer<KMariaDBContainer>(image)

    companion object {
        private const val DATABASE = "codekvast"
        private const val USERNAME = "codekvastUser"
        private const val PASSWORD = "codekvastPassword"

        @Container
        val mariaDB = KMariaDBContainer("mariadb:10.4")
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withEnv("MYSQL_INITDB_SKIP_TZINFO", "true")!!

        @Container
        val rabbitMQ = RabbitMQContainer("rabbitmq:3.8-management-alpine")
            .withVhost("/")
            .withUser("admin", "secret")!!

        @BeforeAll
        fun beforeTest() {
            System.setProperty("spring.datasource.url", mariaDB.jdbcUrl)
            System.setProperty("spring.datasource.username", USERNAME)
            System.setProperty("spring.datasource.password", PASSWORD)
            System.setProperty("spring.rabbitmq.addresses", rabbitMQ.amqpUrl)
        }

    }

    @Inject
    lateinit var mailSender: MailSender

    @Inject
    lateinit var weedingTask: WeedingTask

    @Inject
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun should_send_mail() {
        val recipient = System.getenv("RUN_MAIL_TESTS_SEND_TO")
        assumeTrue(recipient != null)
        mailSender.sendMail(MailSender.Template.WELCOME_TO_CODEKVAST, recipient, 1L)
    }

    @Test
    @Sql(scripts = ["/sql/base-data.sql", "/sql/garbage-data.sql"])
    fun should_perform_dataWeeding() {

        // given
        assertThat(rows("invocations"), `is`(2))
        assertThat(rows("applications"), `is`(5))
        assertThat(rows("environments"), `is`(6))
        assertThat(rows("method_locations"), `is`(6))
        assertThat(rows("methods"), `is`(10))
        assertThat(rows("jvms"), `is`(5))
        assertThat(rows("agent_state"), `is`(5))

        // when
        weedingTask.performDataWeeding()

        // then
        assertThat(rows("invocations"), `is`(1))
        assertThat(rows("applications"), `is`(4))
        assertThat(rows("environments"), `is`(5))
        assertThat(rows("method_locations"), `is`(3))
        assertThat(rows("methods"), `is`(1))
        assertThat(rows("jvms"), `is`(4))
        assertThat(rows("agent_state"), `is`(4))
    }

    @Test
    @Sql(scripts = ["/sql/base-data.sql", "/sql/weedable-data.sql"])
    fun should_find_weeding_candidates() {

        // given
        assertThat(rows("applications"), not(`is`(0)))
        assertThat(rows("environments"), not(`is`(0)));
        assertThat(rows("methods"), not(`is`(0)))
        assertThat(rows("method_locations"), not(`is`(0)))
        assertThat(rows("jvms"), `is`(4))
        assertThat(rows("agent_state"), `is`(4))

        // when
        weedingTask.performDataWeeding()

        // then
        assertThat(rows("invocations"), `is`(0))
        assertThat(rows("applications"), `is`(0))
        assertThat(rows("environments"), `is`(1))
        assertThat(rows("methods"), `is`(0))
        assertThat(rows("method_locations"), `is`(0))
        assertThat(rows("jvms"), `is`(0))
        assertThat(rows("agent_state"), `is`(0))
    }

    private fun rows(tableName: String, vararg args: Any) =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(0) FROM $tableName",
            Int::class.java,
            *args
        )

}
