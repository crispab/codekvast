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
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorStatusMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.internal.InvocationDataReceivedEvent;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.test.matchers.ApplicationStatisticsMatcher.isApplicationStatistics;
import static se.crisp.codekvast.test.matchers.TimestampIsInRangeMatcher.timestampAfter;

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

    private ApplicationStatisticsMessage lastApplicationStatisticsMessage;
    private CollectorStatusMessage lastCollectorStatusMessage;

    @Subscribe
    public void onCollectorStatusMessage(CollectorStatusMessage message) {
        events.add(message);
        lastCollectorStatusMessage = message;
    }

    @Subscribe
    public void onApplicationStatisticsMessage(ApplicationStatisticsMessage message) {
        events.add(message);
        lastApplicationStatisticsMessage = message;
    }

    @Subscribe
    public void onInvocationDataReceivedEvent(InvocationDataReceivedEvent event) {
        events.add(event);
    }

    @Test
    public void testStoreJvmData_fromValidAgent() throws Exception {
        // given
        long collectionIntervalMillis = 3600_000L;
        long now = System.currentTimeMillis();
        long t0 = now - 3 * collectionIntervalMillis;
        long t1 = now - 2 * collectionIntervalMillis;
        long t2 = now - 1 * collectionIntervalMillis;
        long t3 = now + 0 * collectionIntervalMillis;
        long networkLatencyToleranceMillis = 25L;

        // when
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "uuid1", "agentHostName1"));

        // then
        assertThat(lastApplicationStatisticsMessage, isApplicationStatistics(
                allOf(
                        hasProperty("firstDataReceivedAtMillis", timestampAfter(t0, networkLatencyToleranceMillis)),
                        hasProperty("lastDataReceivedAtMillis", timestampAfter(t1, networkLatencyToleranceMillis)),
                        hasProperty("upTimeSeconds", is((t1 - t0) / 1000))
                )
        ));

        // when
        agentService.storeJvmData("agent", createJvmData(t0, t2, "app1", "uuid1", "agentHostName1"));
        agentService.storeJvmData("agent", createJvmData(t1, t3, "app1", "uuid2", "agentHostName2"));

        // then
        assertThat(countRows(
                "jvm_info WHERE jvm_uuid= ? AND started_at_millis BETWEEN ? AND ? AND reported_at_millis BETWEEN ? AND ? ",
                "uuid1", t0, t0 + networkLatencyToleranceMillis, t2, t2 + networkLatencyToleranceMillis), is(1));

        assertThat(countRows(
                "jvm_info WHERE jvm_uuid= ? AND started_at_millis BETWEEN ? AND ? AND reported_at_millis BETWEEN ? AND ? ",
                "uuid2", t1, t1 + networkLatencyToleranceMillis, t3, t3 + networkLatencyToleranceMillis), is(1));

        assertThat(events, contains(
                instanceOf(ApplicationStatisticsMessage.class),
                instanceOf(CollectorStatusMessage.class),
                instanceOf(ApplicationStatisticsMessage.class),
                instanceOf(CollectorStatusMessage.class),
                instanceOf(ApplicationStatisticsMessage.class),
                instanceOf(CollectorStatusMessage.class)));

        assertThat(lastApplicationStatisticsMessage, isApplicationStatistics(
                allOf(
                        hasProperty("firstDataReceivedAtMillis", timestampAfter(t0, networkLatencyToleranceMillis)),
                        hasProperty("lastDataReceivedAtMillis", timestampAfter(t3, networkLatencyToleranceMillis)),
                        hasProperty("upTimeSeconds", is(average(t2 - t0, t3 - t1) / 1000))
                )
        ));

        CollectorStatusMessage csm = lastCollectorStatusMessage;
        CollectorDisplay collector = csm.getCollectors().iterator().next();
        assertThat(collector.getCollectorStartedAtMillis(), timestampAfter(t1, networkLatencyToleranceMillis));
        assertThat(collector.getDataReceivedAtMillis(), timestampAfter(t3, networkLatencyToleranceMillis));
    }

    private long average(long... values) {
        long sum = 0L;
        for (long value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    @Test(expected = UndefinedUserException.class)
    public void testStoreJvmData_fromUnknownAgent() throws Exception {
        long now = System.currentTimeMillis();
        agentService.storeJvmData("foobar", createJvmData(now, now, "app2", "uuid1", "agentHostName"));
    }

    @Test
    public void testStoreInvocationData() throws Exception {
        long now = System.currentTimeMillis();
        long startedAtMillis = now - 3600_000L;

        agentService.storeJvmData("agent", createJvmData(startedAtMillis, now, "app1", "uuid1.1", "agentHostName"));
        agentService.storeJvmData("agent", createJvmData(startedAtMillis + 100L, now, "app1", "uuid1.2", "agentHostName"));
        agentService.storeJvmData("agent", createJvmData(startedAtMillis - 100L, now, "app2", "uuid2.1", "agentHostName"));
        agentService.storeJvmData("agent", createJvmData(startedAtMillis, now, "app2", "uuid2.2", "agentHostName"));

        assertThat(events, hasSize(8));

        List<SignatureEntry> signatures = new ArrayList<>();
        signatures.add(new SignatureEntry("sig1", 0L, 0L, null));
        signatures.add(new SignatureEntry("sig2", 100L, 100L, SignatureConfidence.EXACT_MATCH));
        signatures.add(new SignatureEntry("sig1", 200L, 200L, SignatureConfidence.EXACT_MATCH));

        events.clear();
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid1.1").signatures(signatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid1.2").signatures(signatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid2.1").signatures(signatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("uuid2.2").signatures(signatures).build());

        assertThat(events, contains(instanceOf(InvocationDataReceivedEvent.class),
                                    instanceOf(ApplicationStatisticsMessage.class),
                                    instanceOf(InvocationDataReceivedEvent.class),
                                    instanceOf(ApplicationStatisticsMessage.class),
                                    instanceOf(InvocationDataReceivedEvent.class),
                                    instanceOf(ApplicationStatisticsMessage.class),
                                    instanceOf(InvocationDataReceivedEvent.class),
                                    instanceOf(ApplicationStatisticsMessage.class)));
    }

    private JvmData createJvmData(long startedAtMillis, long reportedAtMillis, String appName,
                                  String jvmUuid, String hostName) {

        int agentClockSkewMillis = hostName.hashCode();

        return JvmData.builder()
                      .agentComputerId("agentComputerId")
                      .agentHostName(hostName)
                      .agentUploadIntervalSeconds(300)
                      .agentVcsId("agentVcsId")
                      .agentVersion("agentVersion")
                      .agentTimeMillis(System.currentTimeMillis() - agentClockSkewMillis)
                      .appName(appName)
                      .appVersion("appVersion")
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName("collectorHostName")
                      .collectorResolutionSeconds(600)
                      .collectorVcsId("collectorVcsId")
                      .collectorVersion("collectorVersion")
                      .dumpedAtMillis(reportedAtMillis - agentClockSkewMillis)
                      .jvmUuid(jvmUuid)
                      .methodVisibility("methodVisibility")
                      .startedAtMillis(startedAtMillis - agentClockSkewMillis)
                      .tags("  ")
                      .build();
    }
}
