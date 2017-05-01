package io.codekvast.agent.collector.io.impl;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class InvocationDataPublisherImplTest {

    @Test
    public void should_normalize_invocation() throws Exception {
        String normalized = AbstractInvocationDataPublisher.normalize("public void foo.bar.Sample.grok(int)");
        assertThat(normalized, is("foo.bar.Sample.grok(int)"));
    }
}