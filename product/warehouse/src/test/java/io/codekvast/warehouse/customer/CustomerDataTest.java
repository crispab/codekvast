package io.codekvast.warehouse.customer;

import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CustomerDataTest {

    @Test
    public void should_have_decent_toString_with_trialPeriod() throws Exception {
        long collectionStart = 1503332479695L; // 2017-08-21T16:21:19.695Z
        long trialPeriodEnd = collectionStart + 30 * 24 * 60 * 60 * 1000L;

        CustomerData customerData = CustomerData.builder()
                                                .customerId(17L)
                                                .customerName("foo")
                                                .source("bar")
                                                .pricePlan(PricePlan.of(PricePlanDefaults.DEMO))
                                                .collectionStartedAt(Instant.ofEpochMilli(collectionStart))
                                                .trialPeriodEndsAt(Instant.ofEpochMilli(trialPeriodEnd))
                                                .build();
        assertThat(customerData.toString(),
                   is("CustomerData(customerId=17, customerName=foo, source=bar, pricePlan=PricePlan(name=DEMO, overrideBy=null, " +
                          "note=null, maxMethods=25000, maxNumberOfAgents=1, publishIntervalSeconds=5, pollIntervalSeconds=5, " +
                          "retryIntervalSeconds=5, maxCollectionPeriodDays=-1), collectionStartedAt=2017-08-21T16:21:19.695Z, " +
                          "trialPeriodEndsAt=2017-09-20T16:21:19.695Z)"));
    }

    @Test
    public void should_have_decent_toString_without_trialPeriod() throws Exception {
        CustomerData customerData = CustomerData.builder()
                                                .customerId(17L)
                                                .customerName("foo")
                                                .source("bar")
                                                .pricePlan(PricePlan.of(PricePlanDefaults.DEMO))
                                                .build();
        assertThat(customerData.toString(),
                   is("CustomerData(customerId=17, customerName=foo, source=bar, pricePlan=PricePlan(name=DEMO, overrideBy=null, " +
                          "note=null, maxMethods=25000, maxNumberOfAgents=1, publishIntervalSeconds=5, pollIntervalSeconds=5, " +
                          "retryIntervalSeconds=5, maxCollectionPeriodDays=-1), collectionStartedAt=null, trialPeriodEndsAt=null)"));
    }

}