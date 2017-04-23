package io.codekvast.testsupport.docker;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class DockerContainerTest {

    @Test
    public void should_build_run_command_multiple_ports_multiple_envs() {
        DockerContainer dc = DockerContainer.builder()
                                            .imageName("imageName")
                                            .port("1:1")
                                            .port("2:2")
                                            .env("E1=E1")
                                            .env("E2=E2")
                                            .build();
        assertThat(dc.buildDockerRunCommand(), is("docker run -d -p 1:1 -p 2:2 -e E1=E1 -e E2=E2 imageName"));
    }

    @Test
    public void should_build_run_command_only_image() {
        DockerContainer dc = DockerContainer.builder()
                                            .imageName("imageName")
                                            .build();
        assertThat(dc.buildDockerRunCommand(), is("docker run -d imageName"));
    }

    @Test
    public void should_build_run_command_image_and_args() {
        DockerContainer dc = DockerContainer.builder()
                                            .imageName("imageName")
                                            .args("arg1 arg2")
                                            .build();
        assertThat(dc.buildDockerRunCommand(), is("docker run -d imageName arg1 arg2"));
    }
}
