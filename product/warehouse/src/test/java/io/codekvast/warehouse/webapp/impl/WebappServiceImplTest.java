package io.codekvast.warehouse.webapp.impl;

import io.codekvast.warehouse.customer.CustomerData;
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.PricePlan;
import io.codekvast.warehouse.security.CustomerIdProvider;
import io.codekvast.warehouse.webapp.WebappService;
import io.codekvast.warehouse.webapp.model.status.GetStatusResponse1;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
public class WebappServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerIdProvider customerIdProvider;

    private WebappService webappService;

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        webappService = new WebappServiceImpl(jdbcTemplate, customerIdProvider, customerService);
    }

    @Test
    public void should_getStatus() throws Exception {
        // given
        when(customerIdProvider.getCustomerId()).thenReturn(1L);

        CustomerData customerData = CustomerData.builder()
                                                .customerId(1L)
                                                .customerName("customerName")
                                                .planName(PricePlan.TEST.name())
                                                .source("source")
                                                .build();
        when(customerService.getCustomerDataByCustomerId(eq(1L))).thenReturn(customerData);
        when(customerService.countMethods(eq(1L))).thenReturn(1000);

            // when
        GetStatusResponse1 status = webappService.getStatus();

        // then
        assertNotNull(status);
        assertThat(status.getPricePlan(), is("TEST"));
        assertThat(status.getNumMethods(), is(1000));
        assertThat(status.getNumAgents(), is(0));

        System.out.println("status = " + status);

        verify(customerService).getCustomerDataByCustomerId(eq(1L));
        verify(customerService).countMethods(eq(1L));
        verify(customerIdProvider).getCustomerId();

        verifyNoMoreInteractions(customerService, customerIdProvider);
    }
}