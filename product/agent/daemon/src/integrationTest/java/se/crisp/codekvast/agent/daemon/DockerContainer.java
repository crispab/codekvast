package se.crisp.codekvast.agent.daemon;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
@ToString
public class DockerContainer extends ExternalResource {

    @NonNull
    private final String imageName;

    private final List<String> ports;

    private final List<String> envs;

    private String containerId;

    private Map<Integer, Integer> externalPorts = new HashMap<>();

    @Builder
    private DockerContainer(String imageName, @Singular List<String> ports, @Singular List<String> envs) {
        this.imageName = imageName;
        this.ports = ports;
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
            log.debug("Container started, id={}", containerId);

            for (String port : ports) {
                String[] parts = port.split(":");
                if (parts.length > 2) {
                    throw new IllegalArgumentException("Invalid format of port; specify 'internalPort' or 'externalPort:internalPort'");
                }
                Integer externalPort = parts.length == 2 ? Integer.valueOf(parts[0]) : null;
                Integer internalPort = Integer.valueOf(parts.length == 2 ? parts[1] : port);
                if (externalPort == null || externalPort == 0) {
                    log.debug("Looking up external port for internal port {} ...", internalPort);
                    parts = executeCommand("docker port " + containerId + " " + internalPort).split(":");
                    externalPort = Integer.valueOf(parts[1]);
                }
                externalPorts.put(internalPort, externalPort);
            }

            log.debug("Started container {}, access it on {}", containerId, externalPorts);
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

    public int getExternalPort(int internalPort) {
        Integer port = externalPorts.get(internalPort);
        if (port == null) {
            throw new IllegalArgumentException("Unknown internal port: " + internalPort);
        }
        return port;
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
        sb.append("docker run -d");
        for (String p : ports) {
            sb.append(" -p ").append(p);
        }

        for (String e : envs) {
            sb.append(" -e ").append(e);
        }
        sb.append(" ").append(imageName);
        return sb.toString();
    }

}
