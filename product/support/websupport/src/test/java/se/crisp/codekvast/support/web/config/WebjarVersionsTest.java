package se.crisp.codekvast.support.web.config;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class WebjarVersionsTest {

    private static WebjarVersions webjarVersions = new WebjarVersions();

    @Test
    public void testWebjarsExistingVersion() {
        assertThat(webjarVersions.getVersions().get("sockjsclientVersion"), CoreMatchers.<Object>is("0.3.4"));
    }

}
