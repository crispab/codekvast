package io.codekvast.login.heroku;

import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;

import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class HerokuResourcesControllerTest {

    @Mock
    private CodekvastLoginSettings settings;

    @SuppressWarnings("unused")
    @Mock
    private HerokuService herokuService;

    @InjectMocks
    private HerokuResourcesController controller;

    @Test(expected = BadCredentialsException.class)
    public void should_reject_malformed_basic_auth() {
        // given
        when(settings.getHerokuApiPassword()).thenReturn("password");

        // when
        controller.validateBasicAuth("foobar");

        // then
        // exception!
    }

    @Test(expected = BadCredentialsException.class)
    public void should_reject_invalid_basic_auth() {
        // given
        when(settings.getHerokuApiPassword()).thenReturn("password");

        // when
        controller.validateBasicAuth("Basic foobar");

        // then
        // exception!
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
