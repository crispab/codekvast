package io.codekvast.javaagent.config;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.junit5.extensions.CaptureSystemOutput;
import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@CaptureSystemOutput
class AgentConfigLocatorTest {

  @BeforeEach
  void beforeTest() {
    System.clearProperty(AgentConfigLocator.SYSPROP_CONFIG);
  }

  @AfterEach
  void afterTest() {
    System.clearProperty(AgentConfigLocator.SYSPROP_CONFIG);
  }

  @Test
  void should_handle_valid_file(OutputCapture outputCapture) {
    System.setProperty(AgentConfigLocator.SYSPROP_CONFIG, "src/test/resources/codekvast1.conf");
    assertThat(AgentConfigLocator.locateConfig(), not(nullValue()));
    outputCapture.expect(containsString("Found src/test/resources/codekvast1.conf"));
  }

  @Test
  void should_handle_invalid_file(OutputCapture outputCapture) {
    String location = "src/test/resources/codekvast1.conf-FOOBAR";
    System.setProperty(AgentConfigLocator.SYSPROP_CONFIG, location);
    assertThat(AgentConfigLocator.locateConfig(), nullValue());
    outputCapture.expect(containsString("Invalid value of"));
    outputCapture.expect(containsString(location));
  }

  @Test
  void should_handle_no_explicits_given(OutputCapture outputCapture) {
    assertThat(AgentConfigLocator.locateConfig(), nullValue());
    outputCapture.expect(containsString("WARN"));
    outputCapture.expect(containsString(AgentConfigLocator.class.getSimpleName()));
    outputCapture.expect(containsString("No configuration file found"));
    outputCapture.expect(containsString("Codekvast will not start"));
  }
}
