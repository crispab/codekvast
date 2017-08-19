package io.codekvast.warehouse.customer;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CustomerDataTest {

    @Test
    public void should_have_decent_toString() throws Exception {
        CustomerData customerData = CustomerData.builder()
                                                .customerId(17L)
                                                .customerName("foo")
                                                .source("bar")
                                                .pricePlan(PricePlan.of(PricePlanDefaults.DEMO))
                                                .build();
        assertThat(customerData.toString(), is("CustomerData(customerId=17, customerName=foo, source=bar, pricePlan=PricePlan(name=DEMO, overrideBy=null, note=null, maxMethods=25000, maxNumberOfAgents=1, publishIntervalSeconds=5, pollIntervalSeconds=5, retryIntervalSeconds=5, maxCollectionPeriodDays=-1))"));
    }

}