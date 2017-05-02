package io.codekvast.agent.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class PublishingUtilsTest {

    @Test
    public void should_strip_existing_modifiers() throws Exception {
        String stripped = PublishingUtils
            .stripModifiers("public static strictfp void hudson.ClassicPluginStrategy.2(java.io.OutputStream, long)");
        assertThat(stripped, is("hudson.ClassicPluginStrategy.2(java.io.OutputStream, long)"));
    }

    @Test
    public void should_strip_missing_modifiers() throws Exception {
        String stripped = PublishingUtils.stripModifiers("hudson.ClassicPluginStrategy.2(java.io.OutputStream, long)");
        assertThat(stripped, is("hudson.ClassicPluginStrategy.2(java.io.OutputStream, long)"));
    }

    @Test
    public void should_strip_existing_modifiers_no_left_paren() throws Exception {
        String stripped = PublishingUtils.stripModifiers("public synchronized transient foo.bar.Sample.baz");
        assertThat(stripped, is("foo.bar.Sample.baz"));
    }

    @Test
    public void should_strip_missing_modifiers_no_left_paren() throws Exception {
        String stripped = PublishingUtils.stripModifiers("foo.bar.Sample.baz");
        assertThat(stripped, is("foo.bar.Sample.baz"));
    }

    @Test(expected = IllegalStateException.class)
    public void should_strip_missing_modifiers_unbalanced_paren() throws Exception {
        String stripped = PublishingUtils.stripModifiers("foo.bar.Sample.baz)");
        assertThat(stripped, is("foo.bar.Sample.baz"));
    }
}