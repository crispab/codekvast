package se.crisp.codekvast.support.web.config;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class WebjarVersionFilterTest {
    private WebjarVersionFilter filter = new WebjarVersionFilter();

    @Before
    public void beforeTest() throws Exception {
        filter.init(null);
    }

    @Test
    public void testExpandRequestURI_no_webjar() throws Exception {
        assertThat(filter.expandRequestURI("/favicon.ico"), nullValue());
    }

    @Test
    public void testExpandRequestURI_recognized_webjar() throws Exception {
        assertThat(filter.expandRequestURI("/webjars/sockjs-client/sockjs-client.js"), is("/webjars/sockjs-client/0.3.4/sockjs-client.js"));
    }

    @Test
    public void testExpandRequestURI_recognized_webjar_with_explicit_version() throws Exception {
        assertThat(filter.expandRequestURI("/webjars/foo/4.5.6/foo.js"), nullValue());
    }

    @Test
    public void testExpandRequestURI_unrecognized_webjar() throws Exception {
        assertThat(filter.expandRequestURI("/webjars/bar/bar.js"), nullValue());
    }

}
