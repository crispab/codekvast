package io.codekvast.warehouse.agent.impl;

import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.warehouse.agent.AgentService;
import io.codekvast.warehouse.customer.CustomerData;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.LicenseViolationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class AgentServiceImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CustomerService customerService;

    private final CodekvastSettings settings = new CodekvastSettings();
    private final GetConfigRequest1 request = GetConfigRequest1.sample();

    private AgentService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        settings.setQueuePath(temporaryFolder.getRoot());
        service = new AgentServiceImpl(settings, jdbcTemplate, customerService);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyObject())).thenReturn(1);

        when(customerService.getCustomerDataByLicenseKey(anyString())).thenReturn(CustomerData.builder().customerId(1).planName("test").build());
    }

    @Test
    public void should_return_enabled_publishers_when_below_agent_limit() throws Exception {
        GetConfigResponse1 response = service.getConfig(request);

        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=true"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=true"));
    }

    @Test
    public void should_return_disabled_publishers_when_above_agent_limit() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyObject())).thenReturn(10);
        GetConfigResponse1 response = service.getConfig(request);

        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=false"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=false"));
    }

    @Test(expected = LicenseViolationException.class)
    public void should_have_checked_licenseKey() throws Exception {
        long publicationSize = 4711L;
        doThrow(new LicenseViolationException("stub")).when(customerService).assertPublicationSize(anyString(), eq(publicationSize));

        service.saveCodeBasePublication("key", "fingerprint", publicationSize, null);
    }

    @Test
    public void should_save_uploaded_codebase_no_license() throws Exception {
        String contents = "Dummy Code Base Publication";

        File resultingFile = service.saveCodeBasePublication("key",
                                                             "fingerprint",
                                                             1000L,
                                                             new ByteArrayInputStream(contents.getBytes()));

        assertThat(resultingFile, notNullValue());
        assertThat(resultingFile.getName(), startsWith("codebase-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_codebase_licenseKey() throws Exception {
        service.saveCodeBasePublication(null, "fingerprint", 0L, null);
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_invocation_data_licenseKey() throws Exception {
        service.saveInvocationDataPublication(null, "fingerprint", 0L, null);
    }

}