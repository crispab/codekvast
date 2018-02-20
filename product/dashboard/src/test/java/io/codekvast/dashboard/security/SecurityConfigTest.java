package io.codekvast.dashboard.security;

import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.Cookie;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class SecurityConfigTest {

    @Mock
    private SecurityConfig.UnauthorizedHandler unauthorizedHandler;

    @Mock
    private WebappTokenFilter webappTokenFilter;

    private SecurityConfig securityConfig;
    private CodekvastDashboardSettings settings = new CodekvastDashboardSettings();

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        securityConfig = new SecurityConfig(unauthorizedHandler, settings, webappTokenFilter);
    }

    @Test
    public void should_generate_correct_sessionToken_cookie_when_hostHeader_contains_port() {
        // given

        // when
        Cookie cookie = securityConfig.createSessionTokenCookie("foo/bar", "some.domain.com:8080");

        // then
        assertThat(cookie.getDomain(), is("some.domain.com"));
        assertThat(cookie.getValue(), is("foo%2Fbar"));
        assertThat(cookie.getPath(), is("/"));
        assertThat(cookie.getMaxAge(), is(-1));
        assertThat(cookie.isHttpOnly(), is(true));
    }

    @Test
    public void should_generate_correct_sessionToken_cookie_when_hostHeader_lacks_port() {
        // given

        // when
        Cookie cookie = securityConfig.createSessionTokenCookie("foo/bar", "some.domain.com");

        // then
        assertThat(cookie.getDomain(), is("some.domain.com"));
    }

    @Test
    public void should_generate_correct_sessionToken_cookie_when_hostHeader_is_empty() {
        // given

        // when
        Cookie cookie = securityConfig.createSessionTokenCookie("foo/bar", "");

        // then
        assertThat(cookie.getDomain(), is(nullValue()));
    }

    @Test
    public void should_generate_correct_sessionToken_cookie_when_hostHeader_is_null() {
        // given

        // when
        Cookie cookie = securityConfig.createSessionTokenCookie("foo/bar", null);

        // then
        assertThat(cookie.getDomain(), is(nullValue()));
    }
}