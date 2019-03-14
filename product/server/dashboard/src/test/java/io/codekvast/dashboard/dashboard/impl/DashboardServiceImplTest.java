package io.codekvast.dashboard.dashboard.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.customer.PricePlanDefaults;
import io.codekvast.common.security.CustomerIdProvider;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsFormData;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
public class DashboardServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerIdProvider customerIdProvider;

    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private final Instant now = clock.instant();

    private DashboardServiceImpl dashboardService;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        dashboardService = new DashboardServiceImpl(jdbcTemplate, namedParameterJdbcTemplate, customerIdProvider, customerService, clock);
    }

    @Test
    public void should_getStatus_inside_trial_period() {
        // given
        when(customerIdProvider.getCustomerId()).thenReturn(1L);

        PricePlanDefaults ppd = PricePlanDefaults.TEST;
        Instant collectionStartedAt = now.minus(3, DAYS);
        Instant trialPeriodEndsAt = now.plus(3, DAYS);
        CustomerData customerData = CustomerData.builder()
                                                .customerId(1L)
                                                .customerName("customerName")
                                                .pricePlan(PricePlan.of(ppd).toBuilder().retentionPeriodDays(-1).build())
                                                .source("source")
                                                .collectionStartedAt(collectionStartedAt)
                                                .trialPeriodEndsAt(trialPeriodEndsAt)
                                                .build();
        when(customerService.getCustomerDataByCustomerId(eq(1L))).thenReturn(customerData);
        when(customerService.countMethods(eq(1L))).thenReturn(1000);

        // when
        GetStatusResponse status = dashboardService.getStatus();

        // then
        assertNotNull(status);
        assertThat(status.getPricePlan(), is("TEST"));
        assertThat(status.getCollectedSinceMillis(), is(collectionStartedAt.toEpochMilli()));
        assertThat(status.getTrialPeriodEndsAtMillis(), is(trialPeriodEndsAt.toEpochMilli()));
        assertThat(status.getTrialPeriodPercent(), is(50));
        assertThat(status.getTrialPeriodExpired(), is(false));
        assertThat(status.getMaxNumberOfAgents(), is(ppd.getMaxNumberOfAgents()));
        assertThat(status.getMaxNumberOfMethods(), is(ppd.getMaxMethods()));
        assertThat(status.getNumMethods(), is(1000));
        assertThat(status.getNumAgents(), is(0));

        System.out.println("status = " + status);

        verify(customerService).getCustomerDataByCustomerId(eq(1L));
        verify(customerService).countMethods(eq(1L));
        verify(customerIdProvider).getCustomerId();

        verifyNoMoreInteractions(customerService, customerIdProvider);
    }

    @Test
    public void should_getStatus_after_trial_period() {
        // given
        when(customerIdProvider.getCustomerId()).thenReturn(1L);

        PricePlanDefaults ppd = PricePlanDefaults.TEST;
        Instant collectionStartedAt = now.minus(3, DAYS);
        Instant trialPeriodEndsAt = now.minus(1, MILLIS);
        CustomerData customerData = CustomerData.builder()
                                                .customerId(1L)
                                                .customerName("customerName")
                                                .pricePlan(PricePlan.of(ppd).toBuilder().retentionPeriodDays(-1).build())
                                                .source("source")
                                                .collectionStartedAt(collectionStartedAt)
                                                .trialPeriodEndsAt(trialPeriodEndsAt)
                                                .build();
        when(customerService.getCustomerDataByCustomerId(eq(1L))).thenReturn(customerData);
        when(customerService.countMethods(eq(1L))).thenReturn(1000);

        // when
        GetStatusResponse status = dashboardService.getStatus();

        // then
        assertNotNull(status);
        assertThat(status.getCollectedSinceMillis(), is(collectionStartedAt.toEpochMilli()));
        assertThat(status.getTrialPeriodEndsAtMillis(), is(trialPeriodEndsAt.toEpochMilli()));
        assertThat(status.getTrialPeriodPercent(), is(100));
        assertThat(status.getTrialPeriodExpired(), is(true));
    }

    @Test
    public void should_getStatus_not_started_no_trial_period() {
        // given
        when(customerIdProvider.getCustomerId()).thenReturn(1L);

        PricePlanDefaults ppd = PricePlanDefaults.TEST;
        CustomerData customerData = CustomerData.builder()
                                                .customerId(1L)
                                                .customerName("customerName")
                                                .pricePlan(PricePlan.of(ppd))
                                                .source("source")
                                                .collectionStartedAt(null)
                                                .trialPeriodEndsAt(null)
                                                .build();
        when(customerService.getCustomerDataByCustomerId(eq(1L))).thenReturn(customerData);
        when(customerService.countMethods(eq(1L))).thenReturn(1000);

        // when
        GetStatusResponse status = dashboardService.getStatus();

        // then
        assertNotNull(status);
        assertThat(status.getCollectedSinceMillis(), is(nullValue()));
        assertThat(status.getTrialPeriodEndsAtMillis(), is(nullValue()));
        assertThat(status.getTrialPeriodPercent(), is(nullValue()));
        assertThat(status.getTrialPeriodExpired(), is(false));
    }

    @Test
    public void should_getFilterData() {
        // given
        PricePlanDefaults ppd = PricePlanDefaults.TEST;
        long customerId = 1L;

        when(customerIdProvider.getCustomerId()).thenReturn(customerId);
        CustomerData customerData = CustomerData.builder()
                                                .customerId(customerId)
                                                .customerName("customerName")
                                                .pricePlan(PricePlan.of(ppd))
                                                .source("source")
                                                .collectionStartedAt(null)
                                                .trialPeriodEndsAt(null)
                                                .build();
        when(customerService.getCustomerDataByCustomerId(eq(customerId))).thenReturn(customerData);
        //noinspection unchecked
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(customerId)))
            .thenReturn(asList("app2", "app1", "app3"), asList("env2", "env1", "env3"));

        // when
        GetMethodsFormData formData = dashboardService.getMethodsFormData();

        // then
        assertThat(formData, is(GetMethodsFormData.builder()
                                                  .application("app1")
                                                  .application("app2")
                                                  .application("app3")
                                                  .environment("env1")
                                                  .environment("env2")
                                                  .environment("env3")
                                                  .retentionPeriodDays(ppd.getRetentionPeriodDays())
                                                  .build()));
    }
}
