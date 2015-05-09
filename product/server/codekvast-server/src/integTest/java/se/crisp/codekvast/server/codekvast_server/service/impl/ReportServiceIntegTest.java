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

    private final List<SignatureEntry> deadSignatures = asList(
            new SignatureEntry("dead1", 0L, 0L, null),
            new SignatureEntry("dead2", 0L, 0L, null),
            new SignatureEntry("dead3", 0L, 0L, null),
            new SignatureEntry("dead4", 0L, 0L, null));

    @Before
    public void beforeTest() throws CodekvastException {
        // given
        long collectionIntervalMillis = 3600_000L;
        long t0 = now - 10 * collectionIntervalMillis;
        long t1 = now - 9 * collectionIntervalMillis;

        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "1.0", "jvm1", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app1", "1.1", "jvm2", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app2", "2.0", "jvm3", "hostName1"));
        agentService.storeJvmData("agent", createJvmData(t0, t1, "app2", "2.1", "jvm4", "hostName2"));

        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm1").signatures(deadSignatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm2").signatures(deadSignatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm3").signatures(deadSignatures).build());
        agentService.storeSignatureData(SignatureData.builder().jvmUuid("jvm4").signatures(deadSignatures.subList(0, 2)).build());
    }

    @Test
    public void testGetApplicationIds_invalid_organisation_id() throws Exception {
        assertThat(reportDAO.getApplicationIds(0, asList("app1", "app2")), empty());
    }

    @Test
    public void testGetApplicationIds_app1() throws Exception {
        assertThat(reportDAO.getApplicationIds(1, asList("app1")), contains(1L));
    }

    @Test
    public void testGetApplicationIds_app1_twice() throws Exception {
        assertThat(reportDAO.getApplicationIds(1, asList("app1", "app1")), contains(1L));
    }

    @Test
    public void testGetApplicationIds_app2() throws Exception {
        assertThat(reportDAO.getApplicationIds(1, asList("app2")), contains(2L));
    }

    @Test
    public void testGetApplicationIds_all_apps_plus_non_existing() throws Exception {
        assertThat(reportDAO.getApplicationIds(1, asList("app2", "foobar", "app1")), contains(1L, 2L));
    }

    @Test
    public void testGetApplicationIds_only_non_existing() throws Exception {
        assertThat(reportDAO.getApplicationIds(1, asList("foobar")), empty());
    }

    @Test
    public void testGetJvmIds_invalid_organisation_id() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(0, asList("1.0", "2.0")), empty());
    }

    @Test
    public void testGetJvmIds_v1_0() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(1, asList("1.0")), contains(1L));
    }

    @Test
    public void testGetJvmIds_v1_0_twice() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(1, asList("1.0", "1.0")), contains(1L));
    }

    @Test
    public void testGetJvmIds_subset1() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(1, asList("2.0", "1.0")), contains(1L, 3L));
    }

    @Test
    public void testGetJvmIds_subset2() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(1, asList("2.1", "1.1")), contains(2L, 4L));
    }

    @Test
    public void testGetJvmIds_all_plus_non_existing() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(1, asList("2.1", "2.0", "XXX", "1.1", "1.0")), contains(1L, 2L, 3L, 4L));
    }

    @Test
    public void testGetJvmIds_only_non_existing() throws Exception {
        assertThat(reportDAO.getJvmIdsByAppVersions(1, asList("XXX")), empty());
    }

    @Test
    public void testGetDeadMethods1() {
        ReportParameters params = getReportParameters(asList("app1"), asList("1.0"));

        assertThat(reportDAO.getMethodsForScope(MethodUsageScope.DEAD, params), hasSize(4));
    }

    private ReportParameters getReportParameters(List<String> applicationNames, List<String> applicationVersions) {
        return ReportParameters.builder()
                               .organisationId(1)
                               .applicationIds(reportDAO.getApplicationIds(1, applicationNames))
                               .jvmIds(reportDAO.getJvmIdsByAppVersions(1, applicationVersions))
                               .build();
    }

}
