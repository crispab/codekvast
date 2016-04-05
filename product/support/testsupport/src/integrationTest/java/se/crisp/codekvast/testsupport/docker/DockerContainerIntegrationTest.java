package se.crisp.codekvast.testsupport.docker;

import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * @author olle.hallin@crisp.se
 */
public class DockerContainerIntegrationTest {

    @ClassRule
    public static DockerContainer container = DockerContainer.builder()
                                                             .imageName("busybox sleep 10")
                                                             .port("80")
                                                             .build();

    @Test
    public void should_start_container() throws Exception {
        assumeTrue(container.isRunning());

        assertThat(container.getExternalPort(80), not(is(0)));
    }
}
