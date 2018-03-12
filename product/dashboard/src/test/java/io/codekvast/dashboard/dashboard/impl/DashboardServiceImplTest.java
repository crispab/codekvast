package io.codekvast.dashboard.dashboard.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.customer.PricePlanDefaults;
import io.codekvast.common.security.CustomerIdProvider;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import io.codekvast.dashboard.util.TimeService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
public class DashboardServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerIdProvider customerIdProvider;

    @Mock
    private TimeService timeService;

    private Instant now = Instant.now();

    private DashboardServiceImpl webappService;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        webappService = new DashboardServiceImpl(jdbcTemplate, customerIdProvider, customerService, timeService);
        when(timeService.now()).thenReturn(now);
        when(timeService.currentTimeMillis()).thenReturn(now.toEpochMilli());
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
                                                .pricePlan(PricePlan.of(ppd))
                                                .source("source")
                                                .collectionStartedAt(collectionStartedAt)
                                                .trialPeriodEndsAt(trialPeriodEndsAt)
                                                .build();
        when(customerService.getCustomerDataByCustomerId(eq(1L))).thenReturn(customerData);
        when(customerService.countMethods(eq(1L))).thenReturn(1000);

        // when
        GetStatusResponse status = webappService.getStatus();

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
                                                .pricePlan(PricePlan.of(ppd))
                                                .source("source")
                                                .collectionStartedAt(collectionStartedAt)
                                                .trialPeriodEndsAt(trialPeriodEndsAt)
                                                .build();
        when(customerService.getCustomerDataByCustomerId(eq(1L))).thenReturn(customerData);
        when(customerService.countMethods(eq(1L))).thenReturn(1000);

        // when
        GetStatusResponse status = webappService.getStatus();

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
        GetStatusResponse status = webappService.getStatus();

        // then
        assertNotNull(status);
        assertThat(status.getCollectedSinceMillis(), is(nullValue()));
        assertThat(status.getTrialPeriodEndsAtMillis(), is(nullValue()));
        assertThat(status.getTrialPeriodPercent(), is(nullValue()));
        assertThat(status.getTrialPeriodExpired(), is(false));
    }

    @Test
    public void should_detect_synthetic_method_containing_dot_dot() {
        assertThat(webappService.isSyntheticMethod("foo..bar"), is(true));
    }

    @Test
    public void should_not_detect_synthetic_method_not_containing_dot_dot() {
        assertThat(webappService.isSyntheticMethod("foo.bar"), is(false));
    }
}