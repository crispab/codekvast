package io.codekvast.warehouse.customer;

import org.junit.Test;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class PricePlanTest {

    @Test
    public void should_create_from_DEMO_defaults() {
        assertThat(PricePlan.of(PricePlanDefaults.DEMO), not(nullValue()));
    }

    @Test
    public void should_create_from_TEST_defaults() {
        assertThat(PricePlan.of(PricePlanDefaults.TEST), not(nullValue()));
    }
}