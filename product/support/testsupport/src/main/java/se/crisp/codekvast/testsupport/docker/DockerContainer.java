/**
 * Copyright (c) 2015-2016 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package se.crisp.codekvast.testsupport.docker;

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
@Builder
public class DockerContainer extends ExternalResource {

    @NonNull
    private final String imageName;

    @Singular
    private final List<String> ports;

    @Singular
    private final List<String> envs;

    private final ContainerReadyChecker readyChecker;

    private String containerId;

    private final Map<Integer, Integer> externalPorts = new HashMap<>();

    private final boolean leaveContainerRunning;

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
                String externalPort = parts.length == 2 ? parts[0].trim() : "0";
                Integer internalPort = Integer.valueOf(parts.length == 2 ? parts[1] : port);
                if (externalPort.isEmpty() || externalPort.equals("0")) {
                    parts = executeCommand("docker port " + containerId + " " + internalPort).split(":");
                    externalPort = parts[1].trim();
                }
                log.debug("Internal port {} is mapped to {}", internalPort, externalPort);
                externalPorts.put(internalPort, Integer.valueOf(externalPort));
            }

            log.info("Started container {} using ports (internal=external) {}", containerId, externalPorts);

            if (readyChecker != null) {
                waitUntilReady();
            }
        } catch (Exception e) {
            log.error("Cannot execute '" + runCommand + "'", e);
        }
    }

    private void waitUntilReady() {
        long stopWaitingAtMillis = readyChecker.getTimeoutSeconds() <= 0 ? Long.MAX_VALUE :
                System.currentTimeMillis() + readyChecker.getTimeoutSeconds() * 1000L;

        int attempt = 0;
        while (System.currentTimeMillis() < stopWaitingAtMillis) {
            attempt += 1;
            try {
                readyChecker.check(getExternalPort(readyChecker.getInternalPort()));
                return;
            } catch (Exception e) {
                log.debug("{} is not yet ready, attempt #{}", imageName, attempt);
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignore) {
                // ignore
            }
        }

    }

    @Override
    protected void after() {
        if (isRunning()) {
            if (leaveContainerRunning) {
                log.info("Leaving {} running; id={}", imageName, containerId);
                return;
            }

            try {
                log.info("Stopping container {} ...", containerId);
                executeCommand("docker stop " + containerId);

                log.info("Removing container {} ...", containerId);
                executeCommand("docker rm " + containerId);
            } catch (Exception e) {
                log.error("Cannot stop and/or remove docker container", e);
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

    String buildDockerRunCommand() {
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
