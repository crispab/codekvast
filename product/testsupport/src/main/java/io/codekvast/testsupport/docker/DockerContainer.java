/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.testsupport.docker;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.ExternalResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.codekvast.testsupport.ProcessUtils.executeCommand;

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

    private final String args;

    private final boolean pullBeforeRun;

    private final boolean leaveContainerRunning;
    public boolean isRunning() {
        return containerId != null;
    }

    @Override
    protected void before() {
        String runCommand = buildDockerRunCommand();
        try {
            if (pullBeforeRun) {
                executeCommand("docker pull " + imageName);
            }

            containerId = executeCommand(runCommand);
            logger.debug("Container started, id={}", containerId);

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
                logger.debug("Internal port {} is mapped to {}", internalPort, externalPort);
                externalPorts.put(internalPort, Integer.valueOf(externalPort));
            }

            logger.info("Started container {} using ports (internal=external) {}", containerId, externalPorts);

            if (readyChecker != null) {
                waitUntilReady();
            }
        } catch (Exception e) {
            logger.error("Cannot execute '" + runCommand + "'", e);
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
                logger.debug("{} is not yet ready, attempt #{}", imageName, attempt);
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
                logger.info("Leaving {} running; id={}", imageName, containerId);
                return;
            }

            try {
                logger.info("Stopping container {} ...", containerId);
                executeCommand("docker stop " + containerId);

                logger.info("Removing container {} ...", containerId);
                executeCommand("docker rm " + containerId);
            } catch (Exception e) {
                logger.error("Cannot stop and/or remove docker container", e);
            }

        }
    }

    private int getExternalPort(int internalPort) {
        Integer port = externalPorts.get(internalPort);
        if (port == null) {
            throw new IllegalArgumentException("Unknown internal port: " + internalPort);
        }
        return port;
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

        if (args != null) {
            sb.append(" ").append(args);
        }
        return sb.toString();
    }

}
