package io.codekvast.javaagent.publishing.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.publishing.Publisher;
import io.codekvast.junit5.extensions.CaptureSystemOutput;
import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
@CaptureSystemOutput
class AbstractPublisherImplTest {

  private final Publisher publisher = new NoOpCodeBasePublisherImpl(null);

  @Test
  void should_handle_configure_enabled_true(OutputCapture outputCapture) {
    publisher.configure(1L, "enabled=true");

    assertThat(publisher.isEnabled(), is(true));
    outputCapture.expect(containsString("DEBUG"));
    outputCapture.expect(containsString("Setting enabled=true, was=false"));
    outputCapture.expect(containsString("customerId 1"));
  }

  @Test
  void should_handle_configure_enabled_false(OutputCapture outputCapture) {
    publisher.configure(-1L, "enabled=false");
    assertThat(publisher.isEnabled(), is(false));
    outputCapture.expect(containsString("TRACE"));
    outputCapture.expect(containsString("Analyzing enabled=false"));
  }

  @Test
  void should_handle_configure_enabled_foobar(OutputCapture outputCapture) {
    publisher.configure(-1L, "enabled=foobar");
    assertThat(publisher.isEnabled(), is(false));
    outputCapture.expect(containsString("TRACE"));
    outputCapture.expect(containsString("Analyzing enabled=foobar"));
  }

  @Test
  void should_handle_configure_enabled_true_foobar(OutputCapture outputCapture) {
    publisher.configure(0L, "enabled=true; enabled=foobar");
    assertThat(publisher.isEnabled(), is(false));
    outputCapture.expect(containsString("DEBUG"));
    outputCapture.expect(containsString("Setting enabled=true, was=false"));
  }

  @Test
  void should_handle_configure_syntax_error(OutputCapture outputCapture) {
    publisher.configure(0L, "enabled=foo=bar");
    assertThat(publisher.isEnabled(), is(false));
    outputCapture.expect(containsString("WARN"));
    outputCapture.expect(containsString("Illegal key-value pair: enabled=foo=bar"));
  }
}
