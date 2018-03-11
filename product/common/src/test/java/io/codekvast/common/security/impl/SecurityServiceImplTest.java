package io.codekvast.common.security.impl;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.customer.PricePlanDefaults;
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.www.NonceExpiredException;

import java.io.UnsupportedEncodingException;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
public class SecurityServiceImplTest {

    private CodekvastCommonSettings settings = new CodekvastCommonSettingsForTestImpl();

    @Mock
    private CustomerService customerService;

    private SecurityServiceImpl securityService;

    @Before
    public void beforeTest() throws UnsupportedEncodingException {
        MockitoAnnotations.initMocks(this);
        securityService = new SecurityServiceImpl(settings, customerService);
        securityService.postConstruct();
    }


    @Test
    public void should_generate_correct_heroku_sso_token() {
        // Example data from https://devcenter.heroku.com/articles/add-on-single-sign-on

        // given
        String id = "123";
        int timestamp = 1267597772;

        // when
        String token = securityService.makeHerokuSsoToken(id, timestamp);

        // then
        assertThat(token, is("bb466eb1d6bc345d11072c3cd25c311f21be130d"));
    }

    @Test
    public void should_do_Heroku_SSO_when_valid_token_and_timestamp() {
        // given
        Long timestampSeconds = Instant.now().getEpochSecond();
        String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds);

        CustomerData customerData = CustomerData.builder()
                                                .customerId(1L)
                                                .customerName("someCustomerName")
                                                .source("someSource")
                                                .pricePlan(PricePlan.of(PricePlanDefaults.DEMO))
                                                .build();

        when(customerService.getCustomerDataByExternalId("externalId")).thenReturn(customerData);

        // when
        String jwt = securityService.doHerokuSingleSignOn(token, "externalId", "someEmail", timestampSeconds);

        // then
        assertThat(jwt, not(nullValue()));

        verify(customerService).registerLogin(CustomerService.LoginRequest.builder()
                                                                          .customerId(1L)
                                                                          .email("someEmail")
                                                                          .source("someSource")
                                                                          .build());
    }

    @Test(expected = BadCredentialsException.class)
    public void should_reject_Heroku_SSO_when_invalid_token() {
        // given
        Long timestampSeconds = Instant.now().getEpochSecond();
        String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds);

        // when
        securityService.doHerokuSingleSignOn(token + "X", "externalId", "someEmail", timestampSeconds);

        // then
        // Kaboom!
    }

    @Test(expected = NonceExpiredException.class)
    public void should_reject_Heroku_SSO_when_valid_token_too_old_timestamp() {
        // given
        Long timestampSeconds = Instant.now().getEpochSecond();
        String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds);

        // when
        securityService.doHerokuSingleSignOn(token, "externalId", "someEmail", timestampSeconds - 5 * 60 - 1);

        // then
        // Kaboom!
    }

    @Test(expected = NonceExpiredException.class)
    public void should_reject_Heroku_SSO_when_valid_token_too_new_timestamp() {
        // given
        Long timestampSeconds = Instant.now().getEpochSecond();
        String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds);

        // when
        securityService.doHerokuSingleSignOn(token, "externalId", "someEmail", timestampSeconds + 60 + 1);

        // then
        // Kaboom!
    }

    @Getter
    private static class CodekvastCommonSettingsForTestImpl implements CodekvastCommonSettings {

        // Example data from https://devcenter.heroku.com/articles/add-on-single-sign-on
        private String herokuApiSsoSalt = "2f97bfa52ca102f8874716e2eb1d3b4920ad0be4";
        private String webappJwtSecret = "secret";
        private Long webappJwtExpirationHours = 1L;

        // not used in test
        private String applicationName = null;
        private String displayVersion = null;
        private String dnsCname = null;
        private String herokuApiPassword = null;
        private String herokuCodekvastUrl = null;
        private String slackWebHookToken = null;
        private String slackWebHookUrl = null;
    }
}