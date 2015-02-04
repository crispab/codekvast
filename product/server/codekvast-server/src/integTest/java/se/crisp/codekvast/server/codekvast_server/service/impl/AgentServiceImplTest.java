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
import se.crisp.codekvast.server.codekvast_server.config.DatabaseConfig;
import se.crisp.codekvast.server.codekvast_server.config.EventBusConfig;
import se.crisp.codekvast.server.codekvast_server.dao.impl.AgentDAOImpl;
import se.crisp.codekvast.server.codekvast_server.dao.impl.UserDAOImpl;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorDataEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorDataEvent.CollectorEntry;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataReceivedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author Olle Hallin <olle.hallin@crisp.se>
 */
@SuppressWarnings("CastToConcreteClass")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceAutoConfiguration.class, DatabaseConfig.class, EventBusConfig.class,
                                 AgentDAOImpl.class, UserDAOImpl.class, AgentServiceImpl
        .class})
@IntegrationTest({
        "spring.datasource.url=jdbc:h2:mem:serviceTest",
})
public class AgentServiceImplTest extends AbstractServiceTest {

    private static final String JVM_UUID = "uuid";

    @Inject
    private AgentService agentService;

    @Subscribe
    public void onCollectorUptimeEvent(CollectorDataEvent event) {
        events.add(event);
    }

    @Subscribe
    public void onInvocationDataUpdatedEvent(InvocationDataReceivedEvent event) {
        events.add(event);
    }

    @Test
    public void testStoreJvmData_fromValidAgent() throws Exception {
        // given
        long dumpedAtMillis = now - 1000L;

        // when
        agentService.storeJvmData("agent", createJvmData(dumpedAtMillis));
        agentService.storeJvmData("agent", createJvmData(dumpedAtMillis + 1000L));

        // then
        assertThat(countRows("jvm_info WHERE jvm_uuid = ? AND started_at = ? AND dumped_at = ? ", JVM_UUID, startedAtMillis,
                             dumpedAtMillis + 1000L), is(1));

        assertEventsWithinMillis(2, 1000L);

        assertThat(events, hasSize(2));
        assertThat(events.get(0), is(instanceOf(CollectorDataEvent.class)));
        assertThat(events.get(1), is(instanceOf(CollectorDataEvent.class)));

        CollectorDataEvent event = (CollectorDataEvent) events.get(0);
        CollectorEntry collector = event.getCollectors().iterator().next();
        assertThat(collector.getStartedAtMillis(), is(startedAtMillis));
        assertThat(collector.getDumpedAtMillis(), is(dumpedAtMillis));

        event = (CollectorDataEvent) events.get(1);
        collector = event.getCollectors().iterator().next();
        assertThat(collector.getDumpedAtMillis(), is(dumpedAtMillis + 1000L));
    }

    @Test(expected = UndefinedUserException.class)
    public void testStoreJvmData_fromUnknownAgent() throws Exception {
        agentService.storeJvmData("foobar", createJvmData(now));
    }

    @Test
    public void testStoreInvocationData() throws Exception {
        agentService.storeJvmData("agent", createJvmData(now));
        assertEventsWithinMillis(1, 1000L);
        events.clear();

        List<SignatureEntry> signatures = new ArrayList<>();
        signatures.add(new SignatureEntry("sig1", 0L, null));
        signatures.add(new SignatureEntry("sig2", 100L, SignatureConfidence.EXACT_MATCH));
        signatures.add(new SignatureEntry("sig1", 200L, SignatureConfidence.EXACT_MATCH));

        SignatureData data = SignatureData.builder()
                                          .jvmUuid(JVM_UUID)
                                          .signatures(signatures).build();

        agentService.storeSignatureData(data);

        assertEventsWithinMillis(1, 1000L);
        assertThat(events, hasSize(1));
        assertThat(events.get(0), is(instanceOf(InvocationDataReceivedEvent.class)));
    }

    private JvmData createJvmData(long dumpedAtMillis) {
        return JvmData.builder()
                      .agentComputerId("agentComputerId")
                      .agentComputerId("agentComputerId")
                      .agentHostName("agentHostName")
                      .appName(getClass().getName())
                      .appVersion("appVersion")
                      .codekvastVcsId("vcsId")
                      .codekvastVersion("codekvastVersion")
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName("collectorHostName")
                      .collectorResolutionSeconds(600)
                      .dumpedAtMillis(dumpedAtMillis)
                      .jvmUuid(JVM_UUID)
                      .methodExecutionPointcut("methodExecutionPointcut")
                      .startedAtMillis(startedAtMillis)
                      .tags("")
                      .build();
    }
}
