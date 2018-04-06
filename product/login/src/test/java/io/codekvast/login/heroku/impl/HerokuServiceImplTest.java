package io.codekvast.login.heroku.impl;

import io.codekvast.common.customer.CustomerService;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuException;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuServiceImplTest {

    private CodekvastLoginSettings settings = new CodekvastLoginSettings();

    @Mock
    private CustomerService customerService;

    private HerokuServiceImpl service;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        service = new HerokuServiceImpl(settings, customerService);
    }

    @Test
    public void should_delegate_provision_to_customerService_addCustomer() throws HerokuException {
        // given

        // when
        service.provision(HerokuProvisionRequest
                              .builder()
                              .uuid("uuid")
                              .heroku_id("heroku_id")
                              .plan("the-plan")
                              .build());

        // then
        verify(customerService).addCustomer(CustomerService.AddCustomerRequest
                                                .builder()
                                                .source("heroku")
                                                .externalId("uuid")
                                                .name("heroku_id")
                                                .plan("the-plan")
                                                .build());
    }

    @Test
    public void should_delegate_deprovision_to_customerService_deleteCustomerByExternalId() throws HerokuException {
        // given

        // when
        service.deprovision("externalId");

        // then
        verify(customerService).deleteCustomerByExternalId("externalId");
    }
}