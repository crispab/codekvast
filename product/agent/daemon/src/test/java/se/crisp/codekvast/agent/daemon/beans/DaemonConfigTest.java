package se.crisp.codekvast.agent.daemon.beans;

import org.junit.Test;

/**
 * @author olle.hallin@crisp.se
 */
public class DaemonConfigTest {
    @Test
    public void should_create_sample_daemon_config() throws Exception {
        // given

        // when
        DaemonConfig config = DaemonConfig.createSampleDaemonConfig();

        // then
        // should not crash on missing non-null fields
    }
}
