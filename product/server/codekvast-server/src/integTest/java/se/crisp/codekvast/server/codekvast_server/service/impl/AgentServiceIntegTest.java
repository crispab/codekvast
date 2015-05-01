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
import static se.crisp.codekvast.test.matchers.LongIsInRangeMatcher.inRange;

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

    @Subscribe
    public void onCollectorStatusMessage(CollectorStatusMessage message) {
        events.add(message);
    }

    @Subscribe
    public void onApplicationStatisticsMessage(ApplicationStatisticsMessage message) {
        events.add(message);
    }

    @Subscribe
    public void onInvocationDataReceivedEvent(InvocationDataReceivedEvent event) {
        events.add(event);
    }

    @Test
    public void testStoreJvmData_fromValidAgent() throws Exception {
        // given
        long now = System.currentTimeMillis();
        long upTimeMillis = 3600_000L;
        long startedAtMillis = now - upTimeMillis;
        long t1 = now;
        long t2 = now + 1000L;
        long clockSkewToleranceMillis = 25L;

        // when
        agentService.storeJvmData("agent", createJvmData(t1, "app1", "uuid1", startedAtMillis));
        agentService.storeJvmData("agent", createJvmData(t2, "app1", "uuid1", startedAtMillis));

        // then
        assertThat(countRows("jvm_info WHERE jvm_uuid = ? " +
                                     "AND started_at_millis >= ? " +
                                     "AND started_at_millis < ? " +
                                     "AND reported_at_millis >= ? " +
                                     "AND reported_at_millis < ? ",
                             "uuid1", startedAtMillis, startedAtMillis + clockSkewToleranceMillis,
                             t2, t2 + clockSkewToleranceMillis), is(1));

        assertThat(events, contains(
                isApplicationStatistics(startedAtMillis, t1, clockSkewToleranceMillis),
                instanceOf(CollectorStatusMessage.class),
                isApplicationStatistics(startedAtMillis, t2, clockSkewToleranceMillis),
                instanceOf(CollectorStatusMessage.class)));

        CollectorStatusMessage csm = (CollectorStatusMessage) events.get(1);
        CollectorDisplay collector = csm.getCollectors().iterator().next();
        assertThat(collector.getCollectorStartedAtMillis(), inRange(startedAtMillis, startedAtMillis + clockSkewToleranceMillis));
    }

    @Test(expected = UndefinedUserException.class)
    public void testStoreJvmData_fromUnknownAgent() throws Exception {
        long now = System.currentTimeMillis();
        agentService.storeJvmData("foobar", createJvmData(now, "app2", "uuid1", now));
    }

    @Test
    public void testStoreInvocationData() throws Exception {
        long now = System.currentTimeMillis();
        long startedAtMillis = now - 3600_000L;

        agentService.storeJvmData("agent", createJvmData(now, "app1", "uuid1.1", startedAtMillis));
        agentService.storeJvmData("agent", createJvmData(now, "app1", "uuid1.2", startedAtMillis + 100L));
        agentService.storeJvmData("agent", createJvmData(now, "app2", "uuid2.1", startedAtMillis - 100L));
        agentService.storeJvmData("agent", createJvmData(now, "app2", "uuid2.2", startedAtMillis));

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

    private JvmData createJvmData(long reportedAtMillis, String appName, String jvmUuid, long startedAtMillis) {
        return JvmData.builder()
                      .agentComputerId("agentComputerId")
                      .agentHostName("agentHostName")
                      .agentUploadIntervalSeconds(300)
                      .agentVcsId("agentVcsId")
                      .agentVersion("agentVersion")
                      .agentTimeMillis(System.currentTimeMillis())
                      .appName(appName)
                      .appVersion("appVersion")
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName("collectorHostName")
                      .collectorResolutionSeconds(600)
                      .collectorVcsId("collectorVcsId")
                      .collectorVersion("collectorVersion")
                      .dumpedAtMillis(reportedAtMillis)
                      .jvmUuid(jvmUuid)
                      .methodVisibility("methodVisibility")
                      .startedAtMillis(startedAtMillis)
                      .tags("  ")
                      .build();
    }
}
