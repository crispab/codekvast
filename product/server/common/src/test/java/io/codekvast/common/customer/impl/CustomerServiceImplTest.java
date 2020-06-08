package io.codekvast.common.customer.impl;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.customer.PricePlanDefaults;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.SlackService;
import io.codekvast.common.messaging.model.CollectionStartedEvent;
import io.codekvast.common.messaging.model.LicenseViolationEvent;
import io.codekvast.common.metrics.CommonMetricsService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

public class CustomerServiceImplTest {

  private static final Instant NOW = Instant.now();

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private SlackService slackService;

  @Mock private CommonMetricsService metricsService;

  @Mock private EventService eventService;

  private CustomerService service;

  @BeforeEach
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);

    service = new CustomerServiceImpl(jdbcTemplate, slackService, metricsService, eventService);

    Map<String, Object> map = new HashMap<>();
    map.put("id", 1L);
    map.put("createdAt", Timestamp.from(NOW));
    map.put("name", "name");
    map.put("plan", "test");
    map.put("source", "source");

    when(jdbcTemplate.queryForMap(startsWith("SELECT c.id, c.name, c.source"), any()))
        .thenReturn(map);
  }

  @Test
  public void should_return_sensible_CustomerData() {
    // Given

    // When
    CustomerData data = service.getCustomerDataByLicenseKey("key");

    // Then
    assertThat(data.getCustomerId(), is(1L));
    assertThat(data.getPricePlan().getName(), is("TEST"));

    verifyNoInteractions(eventService);
  }

  @Test
  public void should_reject_too_many_methods() {
    // Given
    when(jdbcTemplate.queryForObject(
            startsWith("SELECT COUNT(1) FROM methods WHERE"), eq(Long.class), eq(1L)))
        .thenReturn(50_000L);

    // When
    LicenseViolationException exception =
        assertThrows(LicenseViolationException.class, () -> service.assertDatabaseSize(1L));

    // Then
    assertThat(exception.getMessage(), containsString("Too many methods"));
    assertThat(exception.getMessage(), containsString("50000"));

    verify(eventService).send(any(LicenseViolationEvent.class));
  }

  @Test
  public void should_reject_too_big_codeBasePublication() {
    CustomerData customerData = service.getCustomerDataByLicenseKey("");

    LicenseViolationException exception =
        assertThrows(
            LicenseViolationException.class,
            () -> service.assertPublicationSize(customerData, 100_000));
    assertThat(exception.getMessage(), containsString("100000"));

    verify(eventService).send(any(LicenseViolationEvent.class));
  }

  @Test
  void
      should_update_customers_and_start_trial_period_when_agent_polls_the_first_time_and_trial_period_days() {
    // given
    CustomerData customerData =
        CustomerData.builder()
            .customerId(1L)
            .source("test")
            .customerName("customerName")
            .collectionStartedAt(null)
            .pricePlan(PricePlan.of(PricePlanDefaults.TEST).toBuilder().trialPeriodDays(10).build())
            .build();
    when(jdbcTemplate.update(anyString(), any(Timestamp.class), any(Timestamp.class), anyLong()))
        .thenReturn(1);

    // when
    CustomerData data = service.registerAgentPoll(customerData, NOW);

    // then
    assertThat(data.getCollectionStartedAt(), is(NOW));
    verify(jdbcTemplate)
        .update(
            startsWith("UPDATE customers"),
            eq(Timestamp.from(NOW)),
            eq(Timestamp.from(NOW.plus(10, DAYS))),
            eq(1L));
    verify(eventService).send(any(CollectionStartedEvent.class));
  }

  @Test
  void
      should_update_customers_but_not_start_trial_period_when_agent_polls_the_first_time_and_no_trial_period_days() {
    // given
    CustomerData customerData =
        CustomerData.builder()
            .customerId(1L)
            .source("test")
            .customerName("customerName")
            .collectionStartedAt(null)
            .pricePlan(PricePlan.of(PricePlanDefaults.TEST).toBuilder().trialPeriodDays(-1).build())
            .build();
    when(jdbcTemplate.update(anyString(), any(Timestamp.class), isNull(), anyLong())).thenReturn(1);

    // when
    CustomerData data = service.registerAgentPoll(customerData, NOW);

    // then
    assertThat(data.getCollectionStartedAt(), is(NOW));
    verify(jdbcTemplate)
        .update(startsWith("UPDATE customers SET"), eq(Timestamp.from(NOW)), eq(null), eq(1L));
    verify(eventService).send(any(CollectionStartedEvent.class));
  }

  @Test
  void should_not_update_customers_when_agent_polls_the_second_time() {
    // given
    Instant inThePast = NOW.minus(3, DAYS);
    Instant inTheFuture = NOW.plus(7, DAYS);
    CustomerData customerData =
        CustomerData.builder()
            .customerId(1L)
            .source("test")
            .customerName("customerName")
            .pricePlan(PricePlan.of(PricePlanDefaults.TEST))
            .collectionStartedAt(inThePast)
            .trialPeriodEndsAt(inTheFuture)
            .build();
    // when
    CustomerData data = service.registerAgentPoll(customerData, NOW);

    // then
    assertThat(data.getCollectionStartedAt(), is(inThePast));
    assertThat(data.getTrialPeriodEndsAt(), is(inTheFuture));
    verifyNoInteractions(jdbcTemplate, eventService);
  }
}
