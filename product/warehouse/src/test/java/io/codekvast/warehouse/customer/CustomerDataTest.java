package io.codekvast.warehouse.customer;

import org.junit.Test;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CustomerDataTest {

    private static final Instant COLLECTION_START = Instant.parse("2017-08-21T16:21:19.695Z");
    private static final Instant TRIAL_PERIOD_END = COLLECTION_START.plus(30, DAYS);

    private final Instant now = Instant.now();
    private final CustomerData customerData = CustomerData.builder()
                                                          .customerId(17L)
                                                          .customerName("foo")
                                                          .source("bar")
                                                          .pricePlan(PricePlan.of(PricePlanDefaults.DEMO))
                                                          .build();

    @Test
    public void should_not_be_expired_when_no_trial_period() throws Exception {
        assertThat(customerData.isTrialPeriodExpired(now), is(false));
    }

    @Test
    public void should_be_expired_when_after_trialPeriodEndsAt() throws Exception {
        CustomerData cd = customerData.toBuilder().trialPeriodEndsAt(now.minusSeconds(1)).build();
        assertThat(cd.isTrialPeriodExpired(now), is(true));
    }

    @Test
    public void should_not_be_expired_when_before_trialPeriodEndsAt() throws Exception {
        CustomerData cd = customerData.toBuilder().trialPeriodEndsAt(now.plusSeconds(1)).build();
        assertThat(cd.isTrialPeriodExpired(now), is(false));
    }

    @Test
    public void should_have_decent_toString_without_trialPeriod() throws Exception {
        assertThat(customerData.toString(),
                   is("CustomerData(customerId=17, customerName=foo, source=bar, pricePlan=PricePlan(name=DEMO, overrideBy=null, " +
                          "note=null, maxMethods=25000, maxNumberOfAgents=1, publishIntervalSeconds=5, pollIntervalSeconds=5, " +
                          "retryIntervalSeconds=5, maxCollectionPeriodDays=-1), collectionStartedAt=null, trialPeriodEndsAt=null)"));
    }

    @Test
    public void should_have_decent_toString_with_trialPeriod() throws Exception {

        CustomerData cd = customerData.toBuilder().collectionStartedAt(COLLECTION_START).trialPeriodEndsAt(TRIAL_PERIOD_END).build();
        assertThat(cd.toString(),
                   is("CustomerData(customerId=17, customerName=foo, source=bar, pricePlan=PricePlan(name=DEMO, overrideBy=null, " +
                          "note=null, maxMethods=25000, maxNumberOfAgents=1, publishIntervalSeconds=5, pollIntervalSeconds=5, " +
                          "retryIntervalSeconds=5, maxCollectionPeriodDays=-1), collectionStartedAt=2017-08-21T16:21:19.695Z, " +
                          "trialPeriodEndsAt=2017-09-20T16:21:19.695Z)"));
    }

}