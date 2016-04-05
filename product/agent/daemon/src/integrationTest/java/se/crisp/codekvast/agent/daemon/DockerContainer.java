package se.crisp.codekvast.agent.daemon;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
        return containerId != null;
    }

    @Override
    protected void before() {
        String runCommand = buildDockerRunCommand();
        try {
            containerId = executeCommand(runCommand);

            String[] parts = executeCommand("docker port " + containerId + " " + internalPort).split(":");
            host = parts[0];
            port = Integer.valueOf(parts[1]);

            log.debug("Started container {}, access it on {}:{}", containerId, host, port);
        } catch (Exception e) {
            log.error("Cannot execute '" + runCommand + "'", e);
        }
    }

    @Override
    protected void after() {
        if (isRunning()) {
            try {
                executeCommand("docker stop " + containerId);
                executeCommand("docker rm " + containerId);
            } catch (Exception e) {
                log.error("Cannot stop and remove docker container", e);
            }
        }
    }

    private String executeCommand(String command) throws RuntimeException, IOException, InterruptedException {
        log.debug("Attempting to execute '{}' ...", command);
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = collectProcessOutput(process.getErrorStream());
            throw new RuntimeException(String.format("Could not execute '%s': %s%nExit code=%d", command, error, exitCode));
        }

        return collectProcessOutput(process.getInputStream());
    }

    private String collectProcessOutput(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String newLine = "";
        while ((line = reader.readLine()) != null) {
            sb.append(newLine).append(line);
            newLine = String.format("%n");
        }
        return sb.toString();
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

}
