package io.codekvast.dashboard.agent.impl;

import io.codekvast.common.customer.*;
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.lock.LockTemplate;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.AgentPolledEvent;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.javaagent.model.v2.GetConfigRequest2;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Instant;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.DAYS;
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
    private AgentDAO agentDAO;

    @Mock
    private CustomerService customerService;

    @Mock
    private EventService eventService;

    @Mock
    private LockManager lockManager;

    private final CodekvastDashboardSettings settings = new CodekvastDashboardSettings();
    private final GetConfigRequest2 request = GetConfigRequest2.sample();

    private AgentService service;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);

        settings.setQueuePath(temporaryFolder.getRoot());
        settings.setQueuePathPollIntervalSeconds(60);

        when(lockManager.acquireLock(LockManager.Lock.AGENT_STATE)).thenReturn(Optional.of(LockManager.Lock.AGENT_STATE));
        service = new AgentServiceImpl(settings, customerService, eventService, agentDAO, new LockTemplate(lockManager));

        setupCustomerData(null, null);
    }

    @Test
    public void should_acquire_and_release_lock() {
        // given
        when(agentDAO.getNumOtherAliveAgents(eq(1L), eq(request.getJvmUuid()), any())).thenReturn(1);
        when(agentDAO.isEnvironmentEnabled(eq(1L), eq(request.getJvmUuid()))).thenReturn(TRUE);
        when(lockManager.acquireLock(LockManager.Lock.AGENT_STATE)).thenReturn(Optional.of(LockManager.Lock.AGENT_STATE));

        // when
        val response = service.getConfig(request);

        verify(lockManager).acquireLock(LockManager.Lock.AGENT_STATE);
        verify(lockManager).releaseLock(LockManager.Lock.AGENT_STATE);

        assertThat(response.getCodeBasePublisherConfig(), is("enabled=true"));
    }

    @Test
    public void should_give_up_when_failed_to_acquire_lock() {
        // given
        when(agentDAO.getNumOtherAliveAgents(eq(1L), eq(request.getJvmUuid()), any())).thenReturn(1);
        when(agentDAO.isEnvironmentEnabled(eq(1L), eq(request.getJvmUuid()))).thenReturn(TRUE);
        when(lockManager.acquireLock(LockManager.Lock.AGENT_STATE)).thenReturn(Optional.empty());

        // when
        val response = service.getConfig(request);

        verify(lockManager, never()).releaseLock(LockManager.Lock.AGENT_STATE);

        assertThat(response.getCodeBasePublisherConfig(), is("enabled=false"));
    }

    @Test
    public void should_return_enabled_publishers_when_below_agent_limit_no_trial_period() {
        // given
        when(agentDAO.getNumOtherAliveAgents(eq(1L), eq(request.getJvmUuid()), any())).thenReturn(1);
        when(agentDAO.isEnvironmentEnabled(eq(1L), eq(request.getJvmUuid()))).thenReturn(TRUE);

        // when
        val response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=true"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=true"));

        verify(agentDAO).updateAgentEnabledState(1L, request.getJvmUuid(), true);
        verify(eventService).send(any(AgentPolledEvent.class));
    }

    @Test
    public void should_return_enabled_publishers_when_below_agent_limit_within_trial_period() {
        // given
        Instant now = Instant.now();
        setupCustomerData(now.minus(10, DAYS), now.plus(10, DAYS));
        when(agentDAO.getNumOtherAliveAgents(eq(1L), eq(request.getJvmUuid()), any())).thenReturn(1);
        when(agentDAO.isEnvironmentEnabled(eq(1L), eq(request.getJvmUuid()))).thenReturn(TRUE);

        // when
        val response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=true"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=true"));

        verify(eventService).send(any(AgentPolledEvent.class));
    }

    @Test
    public void should_return_disabled_publishers_when_below_agent_limit_after_trial_period_has_expired() {
        // given
        Instant now = Instant.now();
        setupCustomerData(now.minus(10, DAYS), now.minus(1, DAYS));
        when(agentDAO.getNumOtherAliveAgents(eq(1L), eq(request.getJvmUuid()), any())).thenReturn(1);
        when(agentDAO.isEnvironmentEnabled(eq(1L), eq(request.getJvmUuid()))).thenReturn(TRUE);

        // when
        val response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=false"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=false"));

        verify(eventService).send(any(AgentPolledEvent.class));
    }

    @Test
    public void should_return_disabled_publishers_when_above_agent_limit_no_trial_period() {
        // given
        when(agentDAO.getNumOtherAliveAgents(eq(1L), eq(request.getJvmUuid()), any())).thenReturn(10);
        when(agentDAO.isEnvironmentEnabled(eq(1L), eq(request.getJvmUuid()))).thenReturn(TRUE);

        // when
        val response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=false"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=false"));

        verify(eventService).send(any(AgentPolledEvent.class));
    }

    @Test
    public void should_return_disabled_publishers_when_below_agent_limit_disabled_environment() {
        // given
        when(agentDAO.getNumOtherAliveAgents(eq(1L), eq(request.getJvmUuid()), any())).thenReturn(1);
        when(agentDAO.isEnvironmentEnabled(eq(1L), eq(request.getJvmUuid()))).thenReturn(FALSE);
        when(agentDAO.getEnvironmentName(eq(request.getJvmUuid()))).thenReturn(Optional.of("environment"));

        // when
        val response = service.getConfig(request);

        // then
        assertThat(response.getCodeBasePublisherName(), is("http"));
        assertThat(response.getCodeBasePublisherConfig(), is("enabled=false"));

        assertThat(response.getInvocationDataPublisherName(), is("http"));
        assertThat(response.getInvocationDataPublisherConfig(), is("enabled=false"));

        verify(eventService).send(any(AgentPolledEvent.class));
    }

    @Test(expected = LicenseViolationException.class)
    public void should_have_checked_licenseKey() throws Exception {
        // given
        int publicationSize = 4711;
        doThrow(new LicenseViolationException("stub")).when(customerService)
                                                      .assertPublicationSize(any(CustomerData.class), eq(publicationSize));

        // when
        service.savePublication(AgentService.PublicationType.CODEBASE, "key", publicationSize, null);
    }

    @Test
    public void should_save_uploaded_codebase_no_license() throws Exception {
        // given
        String contents = "Dummy Code Base Publication";

        // when
        File resultingFile = service.savePublication(AgentService.PublicationType.CODEBASE, "key", 1000,
                                                     new ByteArrayInputStream(contents.getBytes()));

        // then
        assertThat(resultingFile, notNullValue());
        assertThat(resultingFile.getName(), startsWith("codebase-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));

        verifyNoInteractions(eventService);
    }

    @Test
    public void should_save_uploaded_invocations_no_license() throws Exception {
        // given
        String contents = "Dummy Code Base Publication";

        // when
        File resultingFile = service.savePublication(AgentService.PublicationType.INVOCATIONS, "key", 1000,
                                                     new ByteArrayInputStream(contents.getBytes()));

        // then
        assertThat(resultingFile, notNullValue());
        assertThat(resultingFile.getName(), startsWith("invocations-"));
        assertThat(resultingFile.getName(), endsWith(".ser"));
        assertThat(resultingFile.exists(), is(true));
        assertThat(resultingFile.length(), is((long) contents.length()));

        verifyNoInteractions(eventService);
    }

    @Test(expected = NullPointerException.class)
    public void should_reject_null_licenseKey() throws Exception {
        service.savePublication(AgentService.PublicationType.CODEBASE, null, 0, null);
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
        when(customerService.registerAgentPoll(any(CustomerData.class), any(Instant.class))).thenReturn(customerData);
    }

}
