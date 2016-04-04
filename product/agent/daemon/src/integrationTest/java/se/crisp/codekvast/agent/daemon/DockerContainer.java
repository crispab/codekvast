package se.crisp.codekvast.agent.daemon;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
@ToString
public class DockerContainer extends ExternalResource {

    @NonNull
    private final String imageName;

    @NonNull
    private final Integer internalPort;

    private final List<String> envs;

    private String containerId;

    @Getter
    private String host;

    @Getter
    private Integer port;

    @Builder
    private DockerContainer(String imageName, Integer internalPort, @Singular List<String> envs) {
        this.imageName = imageName;
        this.internalPort = internalPort;
        this.envs = envs;
    }

    public boolean isRunning() {
        return port != null;
    }

    @Override
    protected void before() {
        String runCommand = buildDockerRunCommand();
        try {
            Process process = Runtime.getRuntime().exec(runCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            containerId = reader.readLine();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Could not execute '{}', exit code={}", runCommand, exitCode);
                return;
            }

            process = Runtime.getRuntime().exec("docker port " + containerId + " " + internalPort);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String[] parts = reader.readLine().split(":");
            process.waitFor();

            host = parts[0];
            port = Integer.valueOf(parts[1]);

            log.debug("Started container {}, access it on {}:{}", containerId, host, port);
        } catch (Exception e) {
            log.error("Cannot execute '" + runCommand + "'", e);
        }
    }

    private String buildDockerRunCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("docker run -d -P");
        for (String e : envs) {
            sb.append(" -e ").append(e);
        }
        sb.append(" ").append(imageName);
        return sb.toString();
    }

    @Override
    protected void after() {
        if (containerId != null) {
            try {
                Runtime.getRuntime().exec("docker stop " + containerId).waitFor();
            } catch (Exception e) {
                log.error("Cannot stop Docker container " + containerId, e);
            }

            try {
                Runtime.getRuntime().exec("docker rm " + containerId).waitFor();
            } catch (Exception e) {
                log.error("Cannot remove Docker container " + containerId, e);
            }
        }
    }
}
