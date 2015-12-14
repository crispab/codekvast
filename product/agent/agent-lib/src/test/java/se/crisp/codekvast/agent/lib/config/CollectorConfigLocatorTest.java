package se.crisp.codekvast.agent.lib.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CollectorConfigLocatorTest {

    @Before
    public void beforeTest() throws Exception {
        System.clearProperty(CollectorConfigLocator.SYSPROP_CONFIG);
        System.clearProperty(CollectorConfigLocator.SYSPROP_HOME);
        System.setProperty(CollectorConfigLocator.SYSPROP_OPTS, "verbose=true");
    }

    @After
    public void afterTest() throws Exception {
        System.clearProperty(CollectorConfigLocator.SYSPROP_OPTS);
    }

    @Test
    public void valid_file() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_CONFIG, "src/test/resources/codekvast1.conf");
        assertThat(CollectorConfigLocator.locateConfig(System.out), not(nullValue()));
    }

    @Test
    public void invalid_file() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_CONFIG, "src/test/resources/codekvast1.conf-FOOBAR");
        assertThat(CollectorConfigLocator.locateConfig(System.out), nullValue());
    }

    @Test
    public void valid_conf_directory() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_CONFIG, "src/test/resources/collectorConfigLocatorTest/conf");
        assertThat(CollectorConfigLocator.locateConfig(System.out), not(nullValue()));
    }

    @Test
    public void valid_home_conf() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_HOME, "src/test/resources/collectorConfigLocatorTest");
        assertThat(CollectorConfigLocator.locateConfig(System.out), not(nullValue()));
    }

    @Test
    public void valid_home() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_HOME, "src/test/resources/collectorConfigLocatorTest/conf");
        assertThat(CollectorConfigLocator.locateConfig(System.out), not(nullValue()));
    }

    @Test
    public void no_hints() throws Exception {
        assertThat(CollectorConfigLocator.locateConfig(System.out), nullValue());
    }


}
