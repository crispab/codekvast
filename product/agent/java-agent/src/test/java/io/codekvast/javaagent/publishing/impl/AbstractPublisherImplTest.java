package io.codekvast.javaagent.publishing.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.publishing.Publisher;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;

/** @author olle.hallin@crisp.se */
public class AbstractPublisherImplTest {

  @Rule public OutputCaptureRule output = new JulAwareOutputCapture();

  private final Publisher publisher = new NoOpCodeBasePublisherImpl(null);

  @Test
  public void should_handle_configure_enabled_true() throws Exception {
    publisher.configure(1L, "enabled=true");

    assertThat(publisher.isEnabled(), is(true));
    output.expect(containsString("[FINE]"));
    output.expect(containsString("Setting enabled=true, was=false"));
    output.expect(containsString("customerId 1"));
  }

  @Test
  public void should_handle_configure_enabled_false() throws Exception {
    publisher.configure(-1L, "enabled=false");
    assertThat(publisher.isEnabled(), is(false));
    output.expect(is(""));
  }

  @Test
  public void should_handle_configure_enabled_foobar() throws Exception {
    publisher.configure(-1L, "enabled=foobar");
    assertThat(publisher.isEnabled(), is(false));
    output.expect(is(""));
  }

  @Test
  public void should_handle_configure_enabled_true_foobar() throws Exception {
    publisher.configure(0L, "enabled=true; enabled=foobar");
    assertThat(publisher.isEnabled(), is(false));
    output.expect(containsString("[FINE]"));
    output.expect(containsString("Setting enabled=true, was=false"));
  }

  @Test
  public void should_handle_configure_syntax_error() throws Exception {
    publisher.configure(0L, "enabled=foo=bar");
    assertThat(publisher.isEnabled(), is(false));
    output.expect(containsString("[WARNING]"));
    output.expect(containsString("Illegal key-value pair: enabled=foo=bar"));
  }
}
