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
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsDisplay;
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

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"CastToConcreteClass", "OverlyCoupledClass"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceAutoConfiguration.class, DatabaseConfig.class, EventBusConfig.class,
                                 AgentDAOImpl.class, UserDAOImpl.class, CodekvastSettings.class,
                                 AgentServiceImpl.class})
@IntegrationTest({
        "spring.datasource.url=jdbc:h2:mem:serviceTest",
})
public class AgentServiceIntegTest extends AbstractServiceIntegTest {

    private static final String JVM_UUID = "uuid";

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
        long dumpedAtMillis = now;

        // when
        agentService.storeJvmData("agent", createJvmData(dumpedAtMillis));
        agentService.storeJvmData("agent", createJvmData(dumpedAtMillis + 1000L));

        // then
        assertThat(countRows("jvm_info WHERE jvm_uuid = ? AND started_at_millis = ? AND reported_at_millis = ? ", JVM_UUID, startedAtMillis,
                             dumpedAtMillis + 1000L), is(1));

        assertEventsWithinMillis(4, 2000L);

        assertThat(events, hasSize(4));
        assertThat(events.get(0), is(instanceOf(ApplicationStatisticsMessage.class)));
        assertThat(events.get(1), is(instanceOf(CollectorStatusMessage.class)));
        assertThat(events.get(2), is(instanceOf(ApplicationStatisticsMessage.class)));
        assertThat(events.get(3), is(instanceOf(CollectorStatusMessage.class)));

        ApplicationStatisticsMessage statsMessage = (ApplicationStatisticsMessage) events.get(0);
        ApplicationStatisticsDisplay stats = statsMessage.getApplications().iterator().next();
        assertThat(stats.getFirstDataReceivedAtMillis(), is(startedAtMillis));
        assertThat(stats.getLastDataReceivedAtMillis(), is(dumpedAtMillis));

        CollectorStatusMessage csm = (CollectorStatusMessage) events.get(1);
        CollectorDisplay collector = csm.getCollectors().iterator().next();
        assertThat(collector.getCollectorStartedAtMillis(), is(startedAtMillis));

        statsMessage = (ApplicationStatisticsMessage) events.get(2);
        stats = statsMessage.getApplications().iterator().next();
        assertThat(stats.getFirstDataReceivedAtMillis(), is(startedAtMillis));
        assertThat(stats.getLastDataReceivedAtMillis(), is(dumpedAtMillis + 1000L));
    }

    @Test(expected = UndefinedUserException.class)
    public void testStoreJvmData_fromUnknownAgent() throws Exception {
        agentService.storeJvmData("foobar", createJvmData(now));
    }

    @Test
    public void testStoreInvocationData() throws Exception {
        agentService.storeJvmData("agent", createJvmData(now));
        assertEventsWithinMillis(1, 2000L);

        List<SignatureEntry> signatures = new ArrayList<>();
        signatures.add(new SignatureEntry("sig1", 0L, 0L, null));
        signatures.add(new SignatureEntry("sig2", 100L, 100L, SignatureConfidence.EXACT_MATCH));
        signatures.add(new SignatureEntry("sig1", 200L, 200L, SignatureConfidence.EXACT_MATCH));

        SignatureData data = SignatureData.builder()
                                          .jvmUuid(JVM_UUID)
                                          .signatures(signatures).build();

        events.clear();
        agentService.storeSignatureData(data);

        assertEventsWithinMillis(2, 2000L);
        assertThat(events, hasSize(2));
        assertThat(events.get(0), is(instanceOf(InvocationDataReceivedEvent.class)));
        assertThat(events.get(1), is(instanceOf(ApplicationStatisticsMessage.class)));
    }

    private JvmData createJvmData(long dumpedAtMillis) {
        return JvmData.builder()
                      .agentComputerId("agentComputerId")
                      .agentHostName("agentHostName")
                      .agentUploadIntervalSeconds(300)
                      .agentVcsId("agentVcsId")
                      .agentVersion("agentVersion")
                      .appName(getClass().getName())
                      .appVersion("appVersion")
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName("collectorHostName")
                      .collectorResolutionSeconds(600)
                      .collectorVcsId("collectorVcsId")
                      .collectorVersion("collectorVersion")
                      .dumpedAtMillis(dumpedAtMillis)
                      .jvmUuid(JVM_UUID)
                      .methodVisibility("methodVisibility")
                      .startedAtMillis(startedAtMillis)
                      .tags("  ")
                      .build();
    }
}
