package io.codekvast.common.security.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.bootstrap.CodekvastCommonSettingsForTestImpl;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.customer.PricePlanDefaults;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.NonceExpiredException;

/** @author olle.hallin@crisp.se */
public class SecurityServiceImplTest {

  private CodekvastCommonSettings settings = new CodekvastCommonSettingsForTestImpl();

  @Mock private CustomerService customerService;

  @Mock private JdbcTemplate jdbcTemplate;

  private SecurityServiceImpl securityService;

  @Before
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);
    securityService = new SecurityServiceImpl(settings, customerService, jdbcTemplate);
    securityService.postConstruct();
  }

  @Test
  public void should_generate_correct_heroku_sso_token() {
    // Example data from https://devcenter.heroku.com/articles/add-on-single-sign-on

    // given
    String id = "123";
    int timestamp = 1267597772;

    // when
    String token =
        securityService.makeHerokuSsoToken(
            id, timestamp, "2f97bfa52ca102f8874716e2eb1d3b4920ad0be4");

    // then
    assertThat(token, is("bb466eb1d6bc345d11072c3cd25c311f21be130d"));
  }

  @Test
  public void should_do_Heroku_SSO_when_valid_token_and_timestamp() {
    // given
    Long timestampSeconds = Instant.now().getEpochSecond();
    String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds, "salt");

    CustomerData customerData =
        CustomerData.builder()
            .customerId(1L)
            .customerName("someCustomerName")
            .source("heroku")
            .pricePlan(PricePlan.of(PricePlanDefaults.DEMO))
            .build();

    when(customerService.getCustomerDataByExternalId("heroku", "externalId"))
        .thenReturn(customerData);

    // when
    String jwt =
        securityService.doHerokuSingleSignOn(
            token, "externalId", "someEmail", timestampSeconds, "salt");

    // then
    assertThat(jwt, not(nullValue()));

    verify(customerService)
        .registerLogin(
            CustomerService.LoginRequest.builder()
                .customerId(1L)
                .email("someEmail")
                .source("heroku")
                .build());
  }

  @Test(expected = BadCredentialsException.class)
  public void should_reject_Heroku_SSO_when_invalid_token() {
    // given
    long timestampSeconds = Instant.now().getEpochSecond();
    String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds, "salt");

    // when
    securityService.doHerokuSingleSignOn(
        token + "X", "externalId", "someEmail", timestampSeconds, "salt");

    // then
    // Kaboom!
  }

  @Test(expected = NonceExpiredException.class)
  public void should_reject_Heroku_SSO_when_valid_token_too_old_timestamp() {
    // given
    long timestampSeconds = Instant.now().getEpochSecond();
    String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds, "salt");

    // when
    securityService.doHerokuSingleSignOn(
        token, "externalId", "someEmail", timestampSeconds - 5 * 60 - 1, "salt");

    // then
    // Kaboom!
  }

  @Test(expected = NonceExpiredException.class)
  public void should_reject_Heroku_SSO_when_valid_token_too_new_timestamp() {
    // given
    long timestampSeconds = Instant.now().getEpochSecond();
    String token = securityService.makeHerokuSsoToken("externalId", timestampSeconds, "salt");

    // when
    securityService.doHerokuSingleSignOn(
        token, "externalId", "someEmail", timestampSeconds + 60 + 2, "salt");

    // then
    // Kaboom!
  }

  @Test
  public void should_mask_string_with_even_length() {
    assertThat(
        SecurityServiceImpl.maskSecondHalf("12345678901234567890"), is("1234567890XXXXXXXXXX"));
  }

  @Test
  public void should_mask_string_with_odd_length() {
    assertThat(
        SecurityServiceImpl.maskSecondHalf("123456789012345678901"), is("1234567890XXXXXXXXXXX"));
  }

  @Test
  public void should_return_null_getCustomerId_when_unauthenticated() {
    securityService.removeAuthentication();
    assertThat(securityService.getCustomerId(), is(nullValue()));
  }

  @Test
  public void should_return_customerId_when_authenticated() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(4711L, "credentials"));
    assertThat(securityService.getCustomerId(), is(4711L));
  }
}
