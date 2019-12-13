package io.codekvast.dashboard.agent.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.messaging.CorrelationIdHolder;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.metrics.PublicationMetricsService;
import io.codekvast.dashboard.model.PublicationType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AgentServiceImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private CustomerService customerService;

    @Mock
    private AgentDAO agentDAO;

    @Mock
    private PublicationMetricsService publicationMetricsService;

    private final CustomerData customerData = CustomerData.sample();

    private AgentService service;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);

        CodekvastDashboardSettings settings = new CodekvastDashboardSettings();
        settings.setFileImportQueuePath(temporaryFolder.getRoot());

        when(customerService.getCustomerDataByLicenseKey(anyString())).thenReturn(customerData);

        service =
            new AgentServiceImpl(settings, customerService, agentDAO, mock(AgentStateManager.class), publicationMetricsService);
    }

    @Test(expected = LicenseViolationException.class)
    public void should_have_checked_licenseKey() throws Exception {
        // given
        int publicationSize = 4711;
        doThrow(new LicenseViolationException("stub")).when(customerService)
                                                      .assertPublicationSize(any(CustomerData.class), eq(publicationSize));

        // when
        service.savePublication(PublicationType.CODEBASE, "key", "fingerprint", publicationSize, null);
    }

    @Test
    public void should_save_uploaded_codebase_no_license() throws Exception {
        // given
        String contents = "Dummy Code Base Publication";

        // when
        File resultingFile = service.savePublication(PublicationType.CODEBASE, "key", "fingerprint",
                                                     1000, new ByteArrayInputStream(contents.getBytes()));

        // then
        assertThat(resultingFile, notNullValue());
        assertThat(service.getPublicationTypeFromPublicationFile(resultingFile), is(PublicationType.CODEBASE));
        assertThat(resultingFile.getName(), startsWith("codebase-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));
    }

    @Test
    public void should_not_save_already_uploaded_codebase() throws Exception {
        // given
        String contents = "Dummy Code Base Publication";
        when(agentDAO.isCodebaseAlreadyImported(customerData.getCustomerId(), "fingerprint")).thenReturn(true);

        // when
        File resultingFile = service.savePublication(PublicationType.CODEBASE, "key", "fingerprint",
                                                     1000, new ByteArrayInputStream(contents.getBytes()));

        // then
        assertThat(resultingFile, nullValue());
        verify(customerService, never()).assertPublicationSize(any(), anyInt());
        verify(publicationMetricsService).countIgnoredPublication(PublicationType.CODEBASE);
    }

    @Test
    public void should_save_uploaded_invocations_no_license() throws Exception {
        // given
        String contents = "Dummy Code Base Publication";

        // when
        File resultingFile = service.savePublication(PublicationType.INVOCATIONS, "key", "fingerprint",
                                                     1000, new ByteArrayInputStream(contents.getBytes()));

        // then
        assertThat(resultingFile, notNullValue());
        assertThat(resultingFile.getName(), startsWith("invocations-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_licenseKey() throws Exception {
        service.savePublication(PublicationType.CODEBASE, null, "fingerprint", 0, null);
    }

    @Test
    public void should_build_file_name_with_correlationId() {
        // given
        String correlationId = CorrelationIdHolder.generateNew();

        // when
        File file = service.generatePublicationFile(PublicationType.CODEBASE, 17L, correlationId);

        // then
        assertThat(file.getName(), containsString(correlationId));

        // when
        String correlationId2 = service.getCorrelationIdFromPublicationFile(file);

        // then
        assertThat(correlationId2, is(correlationId));
    }
}
