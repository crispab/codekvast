package io.codekvast.javaagent.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CollectorConfigLocatorTest {

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Before
    public void beforeTest() throws Exception {
        System.clearProperty(CollectorConfigLocator.SYSPROP_CONFIG);
        System.clearProperty(CollectorConfigLocator.SYSPROP_HOME);
    }

    @After
    public void afterTest() throws Exception {
        System.clearProperty(CollectorConfigLocator.SYSPROP_OPTS);
    }

    @Test
    public void should_handle_valid_file() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_CONFIG, "src/test/resources/codekvast1.conf");
        assertThat(CollectorConfigLocator.locateConfig(), not(nullValue()));
        outputCapture.expect(containsString("Found src/test/resources/codekvast1.conf"));
    }

    @Test
    public void should_handle_invalid_file() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_CONFIG, "src/test/resources/codekvast1.conf-FOOBAR");
        assertThat(CollectorConfigLocator.locateConfig(), nullValue());
        outputCapture.expect(containsString("No configuration file found"));
    }

    @Test
    public void should_handle_valid_conf_directory() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_CONFIG, "src/test/resources/collectorConfigLocatorTest/conf");
        assertThat(CollectorConfigLocator.locateConfig(), not(nullValue()));
    }

    @Test
    public void should_handle_valid_home_conf() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_HOME, "src/test/resources/collectorConfigLocatorTest");
        assertThat(CollectorConfigLocator.locateConfig(), not(nullValue()));
    }

    @Test
    public void should_handle_valid_home() throws Exception {
        System.setProperty(CollectorConfigLocator.SYSPROP_HOME, "src/test/resources/collectorConfigLocatorTest/conf");
        assertThat(CollectorConfigLocator.locateConfig(), not(nullValue()));
    }

    @Test
    public void should_handle_no_hints_given() throws Exception {
        assertThat(CollectorConfigLocator.locateConfig(), nullValue());
        outputCapture.expect(containsString("WARN " + CollectorConfigLocator.class.getName()));
        outputCapture.expect(containsString("No configuration file found"));
    }
}
