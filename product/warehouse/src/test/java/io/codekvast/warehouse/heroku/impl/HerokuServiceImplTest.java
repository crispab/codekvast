package io.codekvast.warehouse.heroku.impl;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.heroku.HerokuProvisionRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuServiceImplTest {

    private CodekvastSettings settings = new CodekvastSettings();

    @Mock
    private CustomerService customerService;

    private HerokuServiceImpl service;

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = new HerokuServiceImpl(settings, customerService);
    }

    @Test
    public void should_delegate_provision_to_customerService_addCustomer() throws Exception {
        // given

        // when
        service.provision(HerokuProvisionRequest
            .builder()
            .uuid("uuid")
            .heroku_id("heroku_id")
            .plan("the-plan")
            .build());

        // then
        verify(customerService).addCustomer(eq(CustomerService.AddCustomerRequest
            .builder()
            .source("heroku")
            .externalId("uuid")
            .name("heroku_id")
            .plan("the-plan")
            .build()));
    }

    @Test
    public void should_delegate_deprovision_to_customerService_deleteCustomerByExternalId() throws Exception {
        // given

        // when
        service.deprovision("externalId");

        // then
        verify(customerService).deleteCustomerByExternalId("externalId");
    }

}