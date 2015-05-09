package se.crisp.codekvast.server.codekvast_server.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.config.DatabaseConfig;
import se.crisp.codekvast.server.codekvast_server.config.EventBusConfig;
import se.crisp.codekvast.server.codekvast_server.dao.ReportDAO;
import se.crisp.codekvast.server.codekvast_server.dao.ReportDAO.ReportParameters;
import se.crisp.codekvast.server.codekvast_server.dao.impl.AgentDAOImpl;
import se.crisp.codekvast.server.codekvast_server.dao.impl.ReportDAOImpl;
import se.crisp.codekvast.server.codekvast_server.dao.impl.UserDAOImpl;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageScope;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.inject.Inject;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"CastToConcreteClass", "OverlyCoupledClass"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DataSourceAutoConfiguration.class, DatabaseConfig.class,
                                 CodekvastSettings.class, AgentDAOImpl.class, UserDAOImpl.class, ReportDAOImpl.class,
                                 EventBusConfig.class, AgentServiceImpl.class, UserServiceImpl.class, ReportServiceImpl.class})
@IntegrationTest({
        "spring.datasource.url = jdbc:h2:mem:serviceTest",
})
public class ReportServiceIntegTest extends AbstractServiceIntegTest {
    @Inject
    private AgentService agentService;

    @Inject
    private ReportDAO reportDAO;

    @Before
    public void beforeTest() throws CodekvastException {
        // given
        long collectionIntervalMillis = 3600_000L;
        long t0 = now - 10 * collectionIntervalMillis;
        long t1 = now - 9 * collectionIntervalMillis;

        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "1.0", "jvm1", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "1.1", "jvm2", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app2", "1.0", "jvm3", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app2", "2.0", "jvm4", "hostName2"));

        List<SignatureEntry> dead = asList(new SignatureEntry("dead1", 0L, 0L, null), new SignatureEntry("dead2", 0L, 0L, null),
                                           new SignatureEntry("dead3", 0L, 0L, null), new SignatureEntry("dead4", 0L, 0L, null));

        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm1").signatures(dead).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm2").signatures(dead).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm3").signatures(dead).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm4").signatures(dead.subList(0, 2)).build());
    }

    @Test
    public void testGetApplicationIds() throws Exception {
        assertThat(reportDAO.getApplicationIds(1, asList("app1", "app2")), contains(1L, 2L));
    }

    @Test
    public void testGetJvmIds() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(1, asList("1.0", "2.0")), containsInAnyOrder(1L, 3L, 4L));
    }

    @Test
    public void testGetDeadMethods() {
        ReportParameters params = ReportParameters.builder()
                                                  .organisationId(1)
                                                  .applicationIds(reportDAO.getApplicationIds(1, asList("app1", "app2")))
                                                  .jvmIds(reportDAO.getJvmIdsByAppVersions(1, asList("1.0", "1.1", "2.0")))
                                                  .build();

        assertThat(reportDAO.getMethodsForScope(MethodUsageScope.DEAD, params), hasSize(4));

    }

}
