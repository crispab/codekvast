package se.crisp.codekvast.server.codekvast_server.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import se.crisp.codekvast.server.codekvast_server.service.DaemonService;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.dao.DaemonDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@ContextConfiguration(classes = {DaemonServiceImpl.class})
public class DaemonDAOIntegTest extends AbstractServiceIntegTest {

    @Inject
    private DaemonService daemonService;

    @Inject
    private DaemonDAO daemonDAO;

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
        daemonService.storeJvmData("daemon", createJvmData(t0, t1, "app1", "1.0", "jvm1", "hostName1"));
        daemonService.storeJvmData("daemon", createJvmData(t0, t1, "app1", "1.1", "jvm2", "hostName1"));
        daemonService.storeJvmData("daemon", createJvmData(t0, t2, "app1", "1.1", "jvm2", "hostName1"));
        daemonService.storeJvmData("daemon", createJvmData(t0, t3, "app1", "1.1", "jvm2", "hostName1"));
        daemonService.storeJvmData("daemon", createJvmData(t2, t3, "app2", "2.0", "jvm3", "hostName1"));
        daemonService.storeJvmData("daemon", createJvmData(t2, t3, "app2", "2.1", "jvm4", "hostName2"));
    }

    @Test
    public void testGetApplicationIds_invalid_organisation_id() throws Exception {
        assertThat(daemonDAO.getApplicationIds(0, asList("app1", "app2")), empty());
    }

    @Test
    public void testGetApplicationIds_app1() throws Exception {
        Collection<AppId> appIds = daemonDAO.getApplicationIds(1, asList("app1"));
        assertThat(appIds, hasSize(2));
    }

    @Test
    public void testGetApplicationIds_app1_twice() throws Exception {
        Collection<AppId> appIds = daemonDAO.getApplicationIds(1, asList("app1", "app1"));
        assertThat(appIds, hasSize(2));
    }

    @Test
    public void testGetApplicationIds_app1_app2() throws Exception {
        Collection<AppId> appIds = daemonDAO.getApplicationIds(1, asList("app2", "app1"));
        assertThat(appIds, hasSize(4));
    }

    @Test
    public void testGetNumCollectors() throws CodekvastException {
        daemonService.storeJvmData("daemon", createJvmData(t2, t3, "app2", "2.1", "jvm5", "hostName3"));
        assertThat(daemonDAO.getNumCollectors(1, "app1"), is(2));
        assertThat(daemonDAO.getNumCollectors(1, "app2"), is(3));
    }

    @Test
    public void testDeleteCollectors() throws Exception {
        daemonService.storeSignatureData(createSignatureData("jvm1", "s1", "s2"));
        daemonService.storeSignatureData(createSignatureData("jvm2", "s3", "s4"));
        daemonService.storeSignatureData(createSignatureData("jvm3", "s1", "s2"));
        daemonService.storeSignatureData(createSignatureData("jvm4", "s3", "s4"));

        assertThat(countRows("applications"), is(2));
        assertThat(countRows("signatures"), is(8));
        assertThat(countRows("jvm_info"), is(4));

        assertThat(daemonDAO.deleteCollectors(1, "app1", "1.0", "hostName1"), is(3));

        assertThat(countRows("applications"), is(2));
        assertThat(countRows("signatures"), is(6));
        assertThat(countRows("jvm_info"), is(3));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testDeleteApplication_whenNotEmpty() throws Exception {
        daemonDAO.deleteApplication(1, "app2");
    }

    @Test
    public void testDeleteApplication_whenEmpty() throws Exception {
        daemonDAO.deleteCollectors(1, "app2", "2.0", "hostName1");
        daemonDAO.deleteCollectors(1, "app2", "2.1", "hostName2");
        assertThat(countRows("applications"), is(2));
        assertThat(countRows("application_statistics"), is(4));

        assertThat(daemonDAO.deleteApplication(1, "app2"), is(3));

        assertThat(countRows("applications"), is(1));
        assertThat(countRows("application_statistics"), is(2));
    }

    private SignatureData createSignatureData(String jvmUuid, String... signatures) {
        return SignatureData.builder()
                            .jvmUuid(jvmUuid)
                            .signatures(Arrays.asList(signatures)
                                              .stream().map(s -> SignatureEntry.builder()
                                                                               .signature(s)
                                                                               .invokedAtMillis(0L)
                                                                               .millisSinceJvmStart(0L)
                                                                               .build())
                                              .collect(toList())).build();
    }
}
