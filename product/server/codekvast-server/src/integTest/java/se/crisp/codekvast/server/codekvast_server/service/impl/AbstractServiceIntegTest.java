package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author olle.hallin@crisp.se
 */
public abstract class AbstractServiceIntegTest extends AbstractTransactionalJUnit4SpringContextTests {
    protected final List<Object> events = new CopyOnWriteArrayList<>();
    protected final long now = System.currentTimeMillis();

    @Inject
    private EventBus eventBus;

    @Before
    public void before() throws Exception {
        eventBus.register(this);
        events.clear();
    }

    @After
    public void after() throws Exception {
        eventBus.unregister(this);
        events.clear();
    }

    protected Integer countRows(String table, Object... args) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class, args);
    }

    protected JvmData createJvmData(long startedAtMillis, long reportedAtMillis, String appName, String appVersion, String jvmUuid,
                                    String hostName) {

        int agentClockSkewMillis = hostName.hashCode() % 300_000;

        return JvmData.builder()
                      .agentHostName(hostName)
                      .agentTimeMillis(System.currentTimeMillis() - agentClockSkewMillis)
                      .appName(appName)
                      .dumpedAtMillis(reportedAtMillis - agentClockSkewMillis)
                      .jvmUuid(jvmUuid)
                .startedAtMillis(startedAtMillis - agentClockSkewMillis)
                        // hard-coded stuff below
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
