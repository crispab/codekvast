package se.crisp.codekvast.warehouse.functest;

import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class WarehouseFunctionTest {

    private final String warehouseEndpoint = System.getProperty("codekvast.warehouse.endpoint");

    @Test
    public void should_have_warehouse_endpoint() throws Exception {
        assertThat(warehouseEndpoint, startsWith("0.0.0.0:"));
    }
}
