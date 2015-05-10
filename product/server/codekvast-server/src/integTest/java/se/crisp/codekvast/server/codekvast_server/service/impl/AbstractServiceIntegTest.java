package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.config.DatabaseConfig;
import se.crisp.codekvast.server.codekvast_server.config.EventBusConfig;
import se.crisp.codekvast.server.codekvast_server.dao.impl.AgentDAOImpl;
import se.crisp.codekvast.server.codekvast_server.dao.impl.ReportDAOImpl;
import se.crisp.codekvast.server.codekvast_server.dao.impl.UserDAOImpl;

import javax.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author olle.hallin@crisp.se
 */
@ContextConfiguration(classes = {
        DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        DatabaseConfig.class, EventBusConfig.class, CodekvastSettings.class,
        AgentDAOImpl.class, UserDAOImpl.class, ReportDAOImpl.class})
@IntegrationTest({
        "spring.datasource.url = jdbc:h2:mem:",
})
@TestExecutionListeners({TransactionalTestExecutionListener.class, SqlScriptsTestExecutionListener.class})
@Transactional
public abstract class AbstractServiceIntegTest extends AbstractJUnit4SpringContextTests {
    protected final List<Object> events = new CopyOnWriteArrayList<>();

    protected final long now = Instant.parse("2015-05-10T10:11:12.456Z").toEpochMilli();

    @Inject
    protected PlatformTransactionManager transactionManager;

    @Inject
    protected JdbcTemplate jdbcTemplate;

    @Inject
    private EventBus eventBus;

    @Before
    public final void before() throws Exception {
        events.clear();
        eventBus.register(this);

        jdbcTemplate.update("ALTER TABLE applications ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE jvm_info ALTER COLUMN id RESTART WITH 1");
    }

    @After
    public final void after() throws Exception {
        try {
            eventBus.unregister(this);
            events.clear();
        } catch (IllegalArgumentException ignore) {

        }
    }

    protected Integer countRows(String table, Object... args) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class, args);
    }

    protected JvmData createJvmData(long startedAtMillis, long reportedAtMillis, String appName, String appVersion, String jvmUuid,
                                    String hostName) {

        return JvmData.builder()
                      .agentHostName(hostName)
                      .agentTimeMillis(-1L)
                      .appName(appName)
                      .dumpedAtMillis(reportedAtMillis)
                      .jvmUuid(jvmUuid)
                      .startedAtMillis(startedAtMillis)

                      .agentComputerId("agentComputerId")
                      .agentUploadIntervalSeconds(300)
                      .agentVcsId("agentVcsId")
                      .agentVersion("agentVersion")
                      .appVersion(appVersion)
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName(hostName)
                      .collectorResolutionSeconds(600)
                      .collectorVcsId("collectorVcsId")
                      .collectorVersion("collectorVersion")
                      .methodVisibility("methodVisibility")
                      .tags("  ")
                      .build();
    }
}
