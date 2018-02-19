package io.codekvast.dashboard.security;

import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.Cookie;

import static org.hamcrest.Matchers.is;
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
    public void should_generate_correct_sessionToken_cookie() {
        // given
        settings.setWebappJwtCookieDomain("Some-Domain");

        // when
        Cookie cookie = securityConfig.createSessionTokenCookie("foo/bar");

        // then
        assertThat(cookie.getDomain(), is("some-domain"));
        assertThat(cookie.getPath(), is("/"));
        assertThat(cookie.getValue(), is("foo%2Fbar"));
        assertThat(cookie.getMaxAge(), is(-1));
        assertThat(cookie.isHttpOnly(), is(true));
    }
}