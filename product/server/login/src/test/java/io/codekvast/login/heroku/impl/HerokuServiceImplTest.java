package io.codekvast.login.heroku.impl;

import com.google.gson.Gson;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuApiWrapper;
import io.codekvast.login.heroku.HerokuDetailsDAO;
import io.codekvast.login.heroku.HerokuException;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
public class HerokuServiceImplTest {

    private CodekvastLoginSettings settings = new CodekvastLoginSettings();

    @Mock
    private HerokuApiWrapper herokuApiWrapper;

    @Mock
    private HerokuDetailsDAO herokuDetailsDAO;

    @Mock
    private CustomerService customerService;

    private final Gson gson = new Gson();

    private HerokuServiceImpl service;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        service = new HerokuServiceImpl(settings, customerService, herokuApiWrapper, herokuDetailsDAO);
    }

    @Test
    public void should_delegate_provision_to_customerService_addCustomer() throws HerokuException {
        // given
        when(customerService.addCustomer(any())).thenReturn(
            CustomerService.AddCustomerResponse.builder()
                                               .customerId(1L)
                                               .licenseKey("licenseKey")
                                               .build());

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
        verify(customerService).deleteCustomerByExternalId("heroku", "externalId");
    }

    @Test
    public void should_deserialize_sample_real_provisioning_request() {
        // given
        HerokuProvisionRequest.OAuthGrant givenOAuthGrant = HerokuProvisionRequest.OAuthGrant.builder()
                                                                                             .code("cc54cdb1-71f9-4098-9a87-3a5abe7d4811")
                                                                                             .expires_at("2018-04-06T02:31:24.18-07:00")
                                                                                             .type("authorization_code")
                                                                                             .build();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/heroku/sample-heroku-provision-request.json")));

        // when
        HerokuProvisionRequest request = gson.fromJson(reader, HerokuProvisionRequest.class);

        // then
        assertThat(request.getUuid(), is("74cc95c3-ad12-4c77-a81c-9ba1c286f9f1"));

        assertThat(request.getOauth_grant(), is(givenOAuthGrant));

        Instant expiresAt = ZonedDateTime.parse(givenOAuthGrant.getExpires_at()).toInstant();
        assertThat(expiresAt.getEpochSecond(), is(1523007084L));
    }
}
