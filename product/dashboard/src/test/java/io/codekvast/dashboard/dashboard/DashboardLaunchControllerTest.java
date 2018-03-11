package io.codekvast.dashboard.dashboard;

import org.junit.Test;

import javax.servlet.http.Cookie;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class DashboardLaunchControllerTest {

    private DashboardLaunchController dashboardLaunchController = new DashboardLaunchController();

    @Test
    public void should_generate_correct_sessionToken_cookie() {
        // given

        // when
        Cookie cookie = dashboardLaunchController.createSessionTokenCookie("foo/bar");

        // then
        assertThat(cookie.getDomain(), is(nullValue()));
        assertThat(cookie.getValue(), is("foo%2Fbar"));
        assertThat(cookie.getPath(), is("/"));
        assertThat(cookie.getMaxAge(), is(-1));
        assertThat(cookie.isHttpOnly(), is(false));
    }
}