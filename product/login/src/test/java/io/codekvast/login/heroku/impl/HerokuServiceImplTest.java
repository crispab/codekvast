package io.codekvast.login.heroku.impl;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuServiceImplTest {

    private CodekvastCommonSettings settings = new CodekvastCommonSettingsForTest();

    @Mock
    private CustomerService customerService;

    private HerokuServiceImpl service;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        service = new HerokuServiceImpl(settings, customerService);
    }

    @Test
    public void should_delegate_provision_to_customerService_addCustomer() {
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
    public void should_delegate_deprovision_to_customerService_deleteCustomerByExternalId() {
        // given

        // when
        service.deprovision("externalId");

        // then
        verify(customerService).deleteCustomerByExternalId("externalId");
    }

    @Data
    private static class CodekvastCommonSettingsForTest implements CodekvastCommonSettings {
        private String applicationName;
        private String displayVersion;
        private String dnsCname;
        private String herokuApiPassword;
        private String herokuApiSsoSalt;
        private String herokuCodekvastUrl;
        private String slackWebHookToken;
        private String slackWebHookUrl;
        private String dashboardJwtSecret;
        private Long dashboardJwtExpirationHours;
    }
}