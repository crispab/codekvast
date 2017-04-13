package se.crisp.codekvast.agent.daemon.beans;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author olle.hallin@crisp.se
 */
public class DaemonConfigTest {
    @Test
    public void should_provide_no_ssh_port() throws Exception {
        // given

        // when
        DaemonConfig config = DaemonConfig.createSampleDaemonConfig();

        // then
        assertThat(config.getUploadToHost(), is((String) null));
        assertThat(config.getUploadToHostOnly(), is((String) null));
        assertThat(config.getUploadToPort(), is(0));
    }

    @Test
    public void should_provide_default_ssh_port() throws Exception {
        // given

        // when
        DaemonConfig config = DaemonConfig.createSampleDaemonConfig().toBuilder().uploadToHost("foobar").build();

        // then
        assertThat(config.getUploadToHost(), is("foobar"));
        assertThat(config.getUploadToHostOnly(), is("foobar"));
        assertThat(config.getUploadToPort(), is(22));
    }

    @Test
    public void should_provide_explicit_ssh_port() throws Exception {
        // given

        // when
        DaemonConfig config = DaemonConfig.createSampleDaemonConfig().toBuilder().uploadToHost("foobar:2222").build();

        // then
        assertThat(config.getUploadToHost(), is("foobar:2222"));
        assertThat(config.getUploadToHostOnly(), is("foobar"));
        assertThat(config.getUploadToPort(), is(2222));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_bad_upload_to_host_format() throws Exception {
        // given

        // when
        DaemonConfig config = DaemonConfig.createSampleDaemonConfig().toBuilder().uploadToHost("foobar:2222:yyy").build();

        // then
        assertThat(config.getUploadToHost(), is("foobar:2222:yyy"));
        assertThat(config.getUploadToHostOnly(), is("foobar"));
        fail("Should have thrown by now");
    }
}
