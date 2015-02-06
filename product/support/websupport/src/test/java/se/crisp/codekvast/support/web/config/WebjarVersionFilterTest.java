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
        filter.getVersions().put("foo", "1.2.3");
    }

    @Test
    public void testGetRequestURI_no_webjar() throws Exception {
        assertThat(filter.expandRequestURI("/favicon.ico"), nullValue());
    }

    @Test
    public void testGetRequestURI_recognized_webjar() throws Exception {
        assertThat(filter.expandRequestURI("/webjars/foo/js/foo.js"), is("/webjars/foo/1.2.3/js/foo.js"));
    }

    @Test
    public void testGetRequestURI_recognized_webjar_with_explicit_version() throws Exception {
        assertThat(filter.expandRequestURI("/webjars/foo/4.5.6/foo.js"), nullValue());
    }

    @Test
    public void testGetRequestURI_unrecognized_webjar() throws Exception {
        assertThat(filter.expandRequestURI("/webjars/bar/bar.js"), nullValue());
    }


}