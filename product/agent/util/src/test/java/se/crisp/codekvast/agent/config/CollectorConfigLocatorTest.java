package se.crisp.codekvast.agent.config;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.agent.config.CollectorConfigLocator.SYSPROP_CONFIG;
import static se.crisp.codekvast.agent.config.CollectorConfigLocator.SYSPROP_HOME;

public class CollectorConfigLocatorTest {

    @Before
    public void beforeTest() throws Exception {
        System.clearProperty(SYSPROP_CONFIG);
        System.clearProperty(SYSPROP_HOME);
    }

    @Test
    public void valid_file() throws Exception {
        System.setProperty(SYSPROP_CONFIG, "src/test/resources/codekvast1.conf");
        assertThat(CollectorConfigLocator.locateConfig(System.out), not(nullValue()));
    }

    @Test
    public void invalid_file() throws Exception {
        System.setProperty(SYSPROP_CONFIG, "src/test/resources/codekvast1.conf-FOOBAR");
        assertThat(CollectorConfigLocator.locateConfig(System.out), nullValue());
    }

    @Test
    public void valid_conf_directory() throws Exception {
        System.setProperty(SYSPROP_CONFIG, "src/test/resources/collectorConfigLocatorTest/conf");
        assertThat(CollectorConfigLocator.locateConfig(System.out), not(nullValue()));
    }

    @Test
    public void valid_home() throws Exception {
        System.setProperty(SYSPROP_HOME, "src/test/resources/collectorConfigLocatorTest");
        assertThat(CollectorConfigLocator.locateConfig(System.out), not(nullValue()));
    }

    @Test
    public void no_hints() throws Exception {
        assertThat(CollectorConfigLocator.locateConfig(System.out), nullValue());
    }


}
