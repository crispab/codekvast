package io.codekvast.login.heroku;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

/** @author olle.hallin@crisp.se */
@ExtendWith(MockitoExtension.class)
public class HerokuResourcesControllerTest {

  @Mock private CodekvastLoginSettings settings;

  @SuppressWarnings("unused")
  @Mock
  private HerokuService herokuService;

  @InjectMocks private HerokuResourcesController controller;

  @Test
  public void should_reject_malformed_basic_auth() {
    // given
    when(settings.getHerokuApiPassword()).thenReturn("password");

    // when, then
    assertThrows(BadCredentialsException.class, () -> controller.validateBasicAuth("foobar"));
  }

  @Test
  public void should_reject_invalid_basic_auth() {
    // given
    when(settings.getHerokuApiPassword()).thenReturn("password");

    // when, then
    assertThrows(BadCredentialsException.class, () -> controller.validateBasicAuth("Basic foobar"));
  }

  @Test
  public void should_accept_valid_basic_auth_mixed_case_prefix() {
    // given
    when(settings.getHerokuApiPassword()).thenReturn("password");

    // when
    controller.validateBasicAuth("BaSiC Y29kZWt2YXN0OnBhc3N3b3Jk");

    // then
    // no exception
  }

  @Test
  public void should_accept_valid_basic_auth_lower_case_prefix() {
    // given
    when(settings.getHerokuApiPassword()).thenReturn("password");

    // when
    controller.validateBasicAuth("basic Y29kZWt2YXN0OnBhc3N3b3Jk");

    // then
    // no exception
  }

  @Test
  public void should_accept_valid_basic_auth_uppercase_case_prefix() {
    // given
    when(settings.getHerokuApiPassword()).thenReturn("password");

    // when
    controller.validateBasicAuth("BASIC Y29kZWt2YXN0OnBhc3N3b3Jk");

    // then
    // no exception
  }
}
