package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.Subscribe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.config.DatabaseConfig;
import se.crisp.codekvast.server.codekvast_server.config.EventBusConfig;
import se.crisp.codekvast.server.codekvast_server.dao.impl.AgentDAOImpl;
import se.crisp.codekvast.server.codekvast_server.dao.impl.UserDAOImpl;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.test.matchers.ApplicationStatisticsMatcher.hasApplicationStatistics;
import static se.crisp.codekvast.test.matchers.CollectorsMatcher.hasCollectors;
import static se.crisp.codekvast.test.matchers.TimestampIsInRangeMatcher.timestampInRange;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"CastToConcreteClass", "OverlyCoupledClass"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceAutoConfiguration.class, DatabaseConfig.class,
                                 CodekvastSettings.class, AgentDAOImpl.class, UserDAOImpl.class,
                                 EventBusConfig.class, AgentServiceImpl.class, UserServiceImpl.class})
@IntegrationTest({
        "spring.datasource.url = jdbc:h2:mem:serviceTest",
})
public class AgentServiceIntegTest extends AbstractServiceIntegTest {
    @Inject
    private AgentService agentService;

    private WebSocketMessage lastWebSocketMessage;

    private final long now = System.currentTimeMillis();

    @Subscribe
    public void onWebSocketMessage(WebSocketMessage message) {
        events.add(message);
        lastWebSocketMessage = message;
    }

    @Test
    public void testStoreJvmData_fromValidAgent() throws Exception {
        // given
        long collectionIntervalMillis = 3600_000L;
        long t0 = now - 10 * collectionIntervalMillis;
        long t1 = now - 9 * collectionIntervalMillis;
        long t2 = now - 8 * collectionIntervalMillis;
        long t3 = now - 7 * collectionIntervalMillis;
        long t4 = now - 6 * collectionIntervalMillis;
        long t5 = now - 5 * collectionIntervalMillis;
        long t6 = now - 4 * collectionIntervalMillis;
        long networkLatencyToleranceMillis = 100L;

        // when an app starts for the first time
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "uuid1", "hostName1"));

        // then
        assertThat(lastWebSocketMessage, hasApplicationStatistics(
                allOf(
                        hasProperty("numHostNames", is(1)),
                        hasProperty("firstDataReceivedAtMillis", timestampInRange(t0, networkLatencyToleranceMillis)),
                        hasProperty("lastDataReceivedAtMillis", timestampInRange(t1, networkLatencyToleranceMillis)),
                        hasProperty("upTimeSeconds", is((t1 - t0) / 1000))
                )
        ));

        // when it continues to execute in the same JVM
        agentService.storeJvmData("agent", createJvmData(t0, t2, "app1", "uuid1", "hostName1"));

        // then
        assertThat(lastWebSocketMessage, hasApplicationStatistics(
                allOf(
                        hasProperty("numHostNames", is(1)),
                        hasProperty("firstDataReceivedAtMillis", timestampInRange(t0, networkLatencyToleranceMillis)),
                        hasProperty("lastDataReceivedAtMillis", timestampInRange(t2, networkLatencyToleranceMillis)),
                        hasProperty("upTimeSeconds", is((t2 - t0) / 1000))
                )
        ));

        // when it restarts in a new JVM in the same host
        agentService.storeJvmData("agent", createJvmData(t3, t4, "app1", "uuid2", "hostName1"));

        // then
        assertThat(lastWebSocketMessage, hasApplicationStatistics(
                allOf(
                        hasProperty("numHostNames", is(1)),
                        hasProperty("firstDataReceivedAtMillis", timestampInRange(t0, networkLatencyToleranceMillis)),
                        hasProperty("lastDataReceivedAtMillis", timestampInRange(t4, networkLatencyToleranceMillis)),
                        hasProperty("upTimeSeconds", is((t2 - t0 + t4 - t3) / 1000))
                )
        ));

        // when a new instance starts in another host
        agentService.storeJvmData("agent", createJvmData(t5, t6, "app1", "uuid3", "hostName2"));

        // then
        assertThat(lastWebSocketMessage, hasApplicationStatistics(
                allOf(
                        hasProperty("numHostNames", is(2)),
                        hasProperty("firstDataReceivedAtMillis", timestampInRange(t0, networkLatencyToleranceMillis)),
                        hasProperty("lastDataReceivedAtMillis", timestampInRange(t6, networkLatencyToleranceMillis)),
                        // average usage time per hostName
                        hasProperty("upTimeSeconds", is((t2 - t0 + t4 - t3 + t6 - t5) / 2 / 1000))
                )
        ));

        assertThat(lastWebSocketMessage, hasCollectors(
                allOf(
                        hasProperty("agentHostname", is("hostName1")),
                        hasProperty("collectorStartedAtMillis", timestampInRange(t3, networkLatencyToleranceMillis)),
                        hasProperty("dataReceivedAtMillis", timestampInRange(t4, networkLatencyToleranceMillis))
                ),
                allOf(
                        hasProperty("agentHostname", is("hostName2")),
                        hasProperty("collectorStartedAtMillis", timestampInRange(t5, networkLatencyToleranceMillis)),
                        hasProperty("dataReceivedAtMillis", timestampInRange(t6, networkLatencyToleranceMillis))
                )
        ));

        assertThat(countRows(
                "jvm_info WHERE jvm_uuid= ? AND started_at_millis BETWEEN ? AND ? AND reported_at_millis BETWEEN ? AND ? ",
                "uuid1", t0, t0 + networkLatencyToleranceMillis, t2, t2 + networkLatencyToleranceMillis), is(1));

        assertThat(countRows(
                "jvm_info WHERE jvm_uuid= ? AND started_at_millis BETWEEN ? AND ? AND reported_at_millis BETWEEN ? AND ? ",
                "uuid2", t3, t3 + networkLatencyToleranceMillis, t4, t4 + networkLatencyToleranceMillis), is(1));

        assertThat(events, hasSize(4));
    }

    @Test(expected = UndefinedUserException.class)
    public void testStoreJvmData_fromUnknownAgent() throws Exception {
        agentService.storeJvmData("foobar", createJvmData(now, now, "app2", "uuid1", "agentHostName"));
    }

    @Test
    public void testStoreInvocationData() throws Exception {
        long startedAtMillis = now - 3600_000L;

        agentService.storeJvmData("agent", createJvmData(startedAtMillis, now, "app1", "uuid1.1", "agentHostName"));
        agentService.storeJvmData("agent", createJvmData(startedAtMillis + 100L, now, "app1", "uuid1.2", "agentHostName"));
        agentService.storeJvmData("agent", createJvmData(startedAtMillis - 100L, now, "app2", "uuid2.1", "agentHostName"));
        agentService.storeJvmData("agent", createJvmData(startedAtMillis, now, "app2", "uuid2.2", "agentHostName"));

        assertThat(events, hasSize(4));

        List<SignatureEntry> signatures = new ArrayList<>();
        signatures.add(new SignatureEntry("sig1", 0L, 0L, null));
        signatures.add(new SignatureEntry("sig2", 100L, 100L, SignatureConfidence.EXACT_MATCH));
        signatures.add(new SignatureEntry("sig1", 200L, 200L, SignatureConfidence.EXACT_MATCH));

        events.clear();
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid1.1").signatures(signatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid1.2").signatures(signatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid2.1").signatures(signatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid2.2").signatures(signatures).build());

        assertThat(events, contains(instanceOf(WebSocketMessage.class),
                                    instanceOf(WebSocketMessage.class),
                                    instanceOf(WebSocketMessage.class),
                                    instanceOf(WebSocketMessage.class)));
    }

    private JvmData createJvmData(long startedAtMillis, long reportedAtMillis, String appName,
                                  String jvmUuid, String hostName) {

        int agentClockSkewMillis = hostName.hashCode() % 300_000;

        return JvmData.builder()
                      .appName(appName)
                      .agentHostName(hostName)
                      .agentTimeMillis(System.currentTimeMillis() - agentClockSkewMillis)
                      .startedAtMillis(startedAtMillis - agentClockSkewMillis)
                      .dumpedAtMillis(reportedAtMillis - agentClockSkewMillis)
                      .jvmUuid(jvmUuid)
                      .agentComputerId("agentComputerId")
                      .agentUploadIntervalSeconds(300)
                      .agentVcsId("agentVcsId")
                      .agentVersion("agentVersion")
                      .appVersion("appVersion")
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
