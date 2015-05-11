package se.crisp.codekvast.server.codekvast_server.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@ContextConfiguration(classes = {AgentServiceImpl.class})
public class AgentDAOIntegTest extends AbstractServiceIntegTest {

    @Inject
    private AgentService agentService;

    @Inject
    private AgentDAO agentDAO;

    private final long bootstrapMillis = 10_000L;
    private final long usageCycleMillis = 600_000L;

    private final long hour = 3600_000L;
    private final long t0 = now - 10 * hour;
    private final long t1 = now - 9 * hour;
    private final long t2 = now - 8 * hour;
    private final long t3 = now - 7 * hour;

    @Before
    public void beforeTest() throws CodekvastException {
        // given
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "1.0", "jvm1", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "1.1", "jvm2", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t2, "app1", "1.1", "jvm2", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t3, "app1", "1.1", "jvm2", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t2, t3, "app2", "2.0", "jvm3", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t2, t3, "app2", "2.1", "jvm4", "hostName2"));
    }

    @Test
    public void testGetApplicationIds_invalid_organisation_id() throws Exception {
        assertThat(agentDAO.getApplicationIds(0, asList("app1", "app2")), empty());
    }

    @Test
    public void testGetApplicationIds_app1() throws Exception {
        Collection<AppId> appIds = agentDAO.getApplicationIds(1, asList("app1"));
        assertThat(appIds, hasSize(2));
    }

    @Test
    public void testGetApplicationIds_app1_twice() throws Exception {
        Collection<AppId> appIds = agentDAO.getApplicationIds(1, asList("app1", "app1"));
        assertThat(appIds, hasSize(2));
    }

    @Test
    public void testGetApplicationIds_app1_app2() throws Exception {
        Collection<AppId> appIds = agentDAO.getApplicationIds(1, asList("app2", "app1"));
        assertThat(appIds, hasSize(4));
    }

}
