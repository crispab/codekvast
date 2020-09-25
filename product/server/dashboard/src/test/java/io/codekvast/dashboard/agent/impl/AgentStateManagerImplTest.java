package io.codekvast.dashboard.agent.impl;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.customer.PricePlanDefaults;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.AgentPolledEvent;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import java.time.Instant;
import java.util.Optional;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AgentStateManagerImplTest {

  private final Long customerId = 1L;
  private final String jvmUuid = "jvmUuid";
  private final String appName = "appName";
  private final String environment = "environment";

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock private AgentDAO agentDAO;

  @Mock private CustomerService customerService;

  @Mock private EventService eventService;

  private final CodekvastDashboardSettings settings = new CodekvastDashboardSettings();

  private CustomerData customerData;

  private AgentStateManager agentStateManager;

  @Before
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);

    settings.setFileImportQueuePath(temporaryFolder.getRoot());
    settings.setFileImportIntervalSeconds(60);

    agentStateManager =
        new AgentStateManagerImpl(settings, customerService, eventService, agentDAO);

    setupCustomerData(null, null);
  }

  @Test
  public void should_return_enabled_publishers_when_failed_to_acquire_lock() {
    // given
    when(agentDAO.getNumOtherAliveAgents(eq(customerId), eq(jvmUuid), any())).thenReturn(1);
    when(agentDAO.isEnvironmentEnabled(eq(customerId), eq(jvmUuid))).thenReturn(TRUE);

    // when
    val response = agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment);

    assertThat(response, is(true));
  }

  @Test
  public void should_return_enabled_publishers_when_below_agent_limit_no_trial_period() {
    // given
    when(agentDAO.getNumOtherAliveAgents(eq(customerId), eq(jvmUuid), any())).thenReturn(1);
    when(agentDAO.isEnvironmentEnabled(eq(customerId), eq(jvmUuid))).thenReturn(TRUE);

    // when
    val response = agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment);

    // then
    assertThat(response, is(true));

    verify(agentDAO).updateAgentEnabledState(customerId, jvmUuid, true);
    verify(eventService).send(any(AgentPolledEvent.class));
  }

  @Test
  public void should_return_enabled_publishers_when_below_agent_limit_within_trial_period() {
    // given
    Instant now = Instant.now();
    setupCustomerData(now.minus(10, DAYS), now.plus(10, DAYS));
    when(agentDAO.getNumOtherAliveAgents(eq(customerId), eq(jvmUuid), any())).thenReturn(1);
    when(agentDAO.isEnvironmentEnabled(eq(customerId), eq(jvmUuid))).thenReturn(TRUE);

    // when
    val response = agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment);

    // then
    assertThat(response, is(true));
    verify(eventService).send(any(AgentPolledEvent.class));
  }

  @Test
  public void
      should_return_disabled_publishers_when_below_agent_limit_after_trial_period_has_expired() {
    // given
    Instant now = Instant.now();
    setupCustomerData(now.minus(10, DAYS), now.minus(1, DAYS));
    when(agentDAO.getNumOtherAliveAgents(eq(customerId), eq(jvmUuid), any())).thenReturn(1);
    when(agentDAO.isEnvironmentEnabled(eq(customerId), eq(jvmUuid))).thenReturn(TRUE);

    // when
    val response = agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment);

    // then
    assertThat(response, is(false));
    verify(eventService).send(any(AgentPolledEvent.class));
  }

  @Test
  public void should_return_disabled_publishers_when_above_agent_limit_no_trial_period() {
    // given
    when(agentDAO.getNumOtherAliveAgents(eq(customerId), eq(jvmUuid), any())).thenReturn(10);
    when(agentDAO.isEnvironmentEnabled(eq(customerId), eq(jvmUuid))).thenReturn(TRUE);

    // when
    val response = agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment);

    // then
    assertThat(response, is(false));
    verify(eventService).send(any(AgentPolledEvent.class));
  }

  @Test
  public void should_return_disabled_publishers_when_below_agent_limit_disabled_environment() {
    // given
    when(agentDAO.getNumOtherAliveAgents(eq(customerId), eq(jvmUuid), any())).thenReturn(1);
    when(agentDAO.isEnvironmentEnabled(eq(customerId), eq(jvmUuid))).thenReturn(FALSE);
    when(agentDAO.getEnvironmentName(eq(jvmUuid))).thenReturn(Optional.of("environment"));

    // when
    val response = agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment);

    // then
    assertThat(response, is(false));
    verify(eventService).send(any(AgentPolledEvent.class));
  }

  private void setupCustomerData(Instant collectionStartedAt, Instant trialPeriodEndsAt) {
    customerData =
        CustomerData.builder()
            .customerId(customerId)
            .customerName("name")
            .source("source")
            .pricePlan(PricePlan.of(PricePlanDefaults.TEST))
            .collectionStartedAt(collectionStartedAt)
            .trialPeriodEndsAt(trialPeriodEndsAt)
            .build();

    when(customerService.getCustomerDataByLicenseKey(anyString())).thenReturn(customerData);
    when(customerService.registerAgentPoll(any(CustomerData.class), any(Instant.class)))
        .thenReturn(customerData);
  }
}
