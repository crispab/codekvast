package se.crisp.codekvast.support.web.config;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WebjarVersionsTest {

    private static WebjarVersions webjarVersions = new WebjarVersions();

    @Test
    public void testWebjarsExistingVersion() {
        assertThat(webjarVersions.getVersions().get("sockjsclientVersion"), is("0.3.4"));
    }

}
