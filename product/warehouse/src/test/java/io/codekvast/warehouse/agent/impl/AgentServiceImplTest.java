package io.codekvast.warehouse.agent.impl;

import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.warehouse.agent.AgentService;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.customer.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.*;
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
        setupCustomerData(null, null);
    }

    @Test
    public void should_return_enabled_publishers_when_below_agent_limit_no_trial_period() throws Exception {
        // given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyObject(), anyString())).thenReturn(1);

        // when
        GetConfigResponse1 response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=true"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=true"));
    }

    @Test
    public void should_return_enabled_publishers_when_below_agent_limit_within_trial_period() throws Exception {
        // given
        Instant now = Instant.now();
        setupCustomerData(now.minus(10, DAYS), now.plus(10, DAYS));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyObject(), anyString())).thenReturn(1);

        // when
        GetConfigResponse1 response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=true"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=true"));
    }

    @Test
    public void should_return_disabled_publishers_when_below_agent_limit_after_trial_period_has_expired() throws Exception {
        // given
        Instant now = Instant.now();
        setupCustomerData(now.minus(10, DAYS), now.minus(1, DAYS));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyObject(), anyString())).thenReturn(1);

        // when
        GetConfigResponse1 response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=false"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=false"));
    }

    @Test
    public void should_return_disabled_publishers_when_above_agent_limit_no_trial_period() throws Exception {
        // given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyObject(), anyString())).thenReturn(10);

        // when
        GetConfigResponse1 response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=false"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=false"));
    }

    @Test(expected = LicenseViolationException.class)
    public void should_have_checked_licenseKey() throws Exception {
        // given
        int publicationSize = 4711;
        doThrow(new LicenseViolationException("stub")).when(customerService).assertPublicationSize(anyString(), eq(publicationSize));

        // when
        service.saveCodeBasePublication("key", "fingerprint", publicationSize, null);
    }

    @Test
    public void should_save_uploaded_codebase_no_license() throws Exception {
        // given
        String contents = "Dummy Code Base Publication";

        // when
        File resultingFile = service.saveCodeBasePublication("key",
                                                             "fingerprint",
                                                             1000,
                                                             new ByteArrayInputStream(contents.getBytes()));

        // then
        assertThat(resultingFile, notNullValue());
        assertThat(resultingFile.getName(), startsWith("codebase-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_codebase_licenseKey() throws Exception {
        service.saveCodeBasePublication(null, "fingerprint", 0, null);
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_invocation_data_licenseKey() throws Exception {
        service.saveInvocationDataPublication(null, "fingerprint", 0, null);
    }

    private void setupCustomerData(Instant collectionStartedAt, Instant trialPeriodEndsAt) {
        CustomerData customerData = CustomerData.builder()
                                         .customerId(1L)
                                         .customerName("name")
                                         .source("source")
                                         .pricePlan(PricePlan.of(PricePlanDefaults.TEST))
                                         .collectionStartedAt(collectionStartedAt)
                                         .trialPeriodEndsAt(trialPeriodEndsAt)
                                         .build();

        when(customerService.getCustomerDataByLicenseKey(anyString())).thenReturn(customerData);
        when(customerService.registerAgentDataPublication(any(CustomerData.class), any(Instant.class))).thenReturn(customerData);
    }

}