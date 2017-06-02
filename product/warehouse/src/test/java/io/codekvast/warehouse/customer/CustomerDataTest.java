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
        CustomerData customerData = CustomerData.builder().customerId(17L).planName("DemO").build();
        assertThat(customerData.toString(), is("CustomerData(customerId=17, planName=DemO)"));
    }

    @Test
    public void should_normalize_planName() throws Exception {
        CustomerData customerData = CustomerData.builder().customerId(17L).planName("dEMo").build();
        assertThat(customerData.getPricePlan(), is(PricePlan.DEMO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_reject_invalid_planName() throws Exception {
        CustomerData customerData = CustomerData.builder().customerId(17L).planName("Invalid").build();
        customerData.getPricePlan();
    }
}