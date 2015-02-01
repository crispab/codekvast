package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.Subscribe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.config.DatabaseConfig;
import se.crisp.codekvast.server.codekvast_server.config.EventBusConfig;
import se.crisp.codekvast.server.codekvast_server.dao.impl.AgentDAOImpl;
import se.crisp.codekvast.server.codekvast_server.dao.impl.UserDAOImpl;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorUptimeEvent;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;

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

    @Inject
    private AgentService agentService;

    @Subscribe
    public void onCollectorUptimeEvent(CollectorUptimeEvent event) {
        synchronized (events) {
            events.add(event);
        }
    }

    @Test
    public void testStoreJvmData_fromValidAgent() throws Exception {
        // given
        long dumpedAtMillis = now - 1000L;

        // when
        agentService.storeJvmData("agent", createJvmData(dumpedAtMillis));
        agentService.storeJvmData("agent", createJvmData(dumpedAtMillis + 1000L));

        // then
        assertThat(countRows("jvm_runs WHERE jvm_fingerprint = ? AND started_at = ? AND dumped_at = ? ", "fingerprint", startedAtMillis,
                             dumpedAtMillis + 1000L), is(1));

        assertEventsWithinMillis(1, 10L);

        assertThat(events, hasSize(2));
        assertThat(events.get(0), is(instanceOf(CollectorUptimeEvent.class)));
        CollectorUptimeEvent event = (CollectorUptimeEvent) events.get(0);

        assertThat(event.getCollectorTimestamp().getStartedAtMillis(), is(startedAtMillis));
        assertThat(event.getCollectorTimestamp().getDumpedAtMillis(), is(dumpedAtMillis));

        event = (CollectorUptimeEvent) events.get(1);
        assertThat(event.getCollectorTimestamp().getDumpedAtMillis(), is(dumpedAtMillis + 1000L));
    }

    @Test(expected = UndefinedUserException.class)
    public void testStoreJvmData_fromUnknownAgent() throws Exception {
        agentService.storeJvmData("foobar", createJvmData(now));
    }

    private JvmData createJvmData(long dumpedAtMillis) {
        return JvmData.builder()
                      .appName(getClass().getName())
                      .appVersion("appVersion")
                      .codekvastVcsId("vcsId")
                      .codekvastVersion("codekvastVersion")
                      .computerId("computerId")
                      .dumpedAtMillis(dumpedAtMillis)
                      .hostName("hostName")
                      .jvmFingerprint("fingerprint")
                      .startedAtMillis(startedAtMillis)
                      .tags("")
                      .build();
    }
}
