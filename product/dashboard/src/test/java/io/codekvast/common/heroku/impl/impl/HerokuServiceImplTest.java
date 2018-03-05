package io.codekvast.common.heroku.impl.impl;

import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.heroku.HerokuProvisionRequest;
import io.codekvast.common.heroku.impl.HerokuServiceImpl;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuServiceImplTest {

    private CodekvastDashboardSettings settings = new CodekvastDashboardSettings();

    @Mock
    private CustomerService customerService;

    private HerokuServiceImpl service;

    @Before
    public void beforeTest() {
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