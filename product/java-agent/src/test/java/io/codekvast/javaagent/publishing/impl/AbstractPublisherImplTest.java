package io.codekvast.javaagent.publishing.impl;

import io.codekvast.javaagent.publishing.Publisher;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class AbstractPublisherImplTest {

    @Rule
    public OutputCapture output = new OutputCapture();

    private final Publisher publisher = new NoOpCodeBasePublisherImpl(null);

    @Test
    public void should_handle_configure_enabled_true() throws Exception {
        publisher.configure("enabled=true");

        assertThat(publisher.isEnabled(), is(true));
        output.expect(containsString("DEBUG"));
        output.expect(containsString("Setting enabled=true, was=false"));
    }

    @Test
    public void should_handle_configure_enabled_false() throws Exception {
        publisher.configure("enabled=false");
        assertThat(publisher.isEnabled(), is(false));
        output.expect(is(""));
    }

    @Test
    public void should_handle_configure_enabled_foobar() throws Exception {
        publisher.configure("enabled=foobar");
        assertThat(publisher.isEnabled(), is(false));
        output.expect(is(""));
    }

    @Test
    public void should_handle_configure_enabled_true_foobar() throws Exception {
        publisher.configure("enabled=true; enabled=foobar");
        assertThat(publisher.isEnabled(), is(false));
        output.expect(containsString("DEBUG"));
        output.expect(containsString("Setting enabled=true, was=false"));
    }

    @Test
    public void should_handle_configure_syntax_error() throws Exception {
        publisher.configure("enabled=foo=bar");
        assertThat(publisher.isEnabled(), is(false));
        output.expect(containsString("WARN"));
        output.expect(containsString("Illegal key-value pair: enabled=foo=bar"));
    }

}