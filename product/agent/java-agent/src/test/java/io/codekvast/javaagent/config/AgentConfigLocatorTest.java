package io.codekvast.javaagent.config;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.publishing.impl.JulAwareOutputCapture;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.springframework.boot.test.system.OutputCaptureRule;

@EnableRuleMigrationSupport
public class AgentConfigLocatorTest {

  @Rule public OutputCaptureRule outputCapture = new JulAwareOutputCapture();

  @BeforeEach
  public void beforeTest() {
    System.clearProperty(AgentConfigLocator.SYSPROP_CONFIG);
  }

  @AfterEach
  public void afterTest() {
    System.clearProperty(AgentConfigLocator.SYSPROP_CONFIG);
  }

  @Test
  public void should_handle_valid_file() {
    System.setProperty(AgentConfigLocator.SYSPROP_CONFIG, "src/test/resources/codekvast1.conf");
    assertThat(AgentConfigLocator.locateConfig(), not(nullValue()));
    outputCapture.expect(containsString("Found src/test/resources/codekvast1.conf"));
  }

  @Test
  public void should_handle_invalid_file() {
    String location = "src/test/resources/codekvast1.conf-FOOBAR";
    System.setProperty(AgentConfigLocator.SYSPROP_CONFIG, location);
    assertThat(AgentConfigLocator.locateConfig(), nullValue());
    outputCapture.expect(containsString("Invalid value of"));
    outputCapture.expect(containsString(location));
  }

  @Test
  public void should_handle_no_explicits_given() {
    assertThat(AgentConfigLocator.locateConfig(), nullValue());
    outputCapture.expect(containsString("[WARNING] " + AgentConfigLocator.class.getName()));
    outputCapture.expect(containsString("No configuration file found"));
  }
}
