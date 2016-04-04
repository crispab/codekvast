/**
 * Copyright (c) 2015-2016 Crisp AB
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
package se.crisp.codekvast.agent.daemon.worker.impl;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.util.LogUtil;
import se.crisp.codekvast.agent.daemon.worker.FileUploadException;
import se.crisp.codekvast.agent.daemon.worker.FileUploader;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static java.util.Optional.ofNullable;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class ScpFileUploaderImpl implements FileUploader {

    private final DaemonConfig config;

    @Inject
    public ScpFileUploaderImpl(DaemonConfig config) {
        this.config = config;
    }

    @Override
    public void validateUploadConfig() throws FileUploadException {
        if (!config.isUploadEnabled()) {
            log.debug("Will not upload");
            return;
        }

        try {
            doValidateUploadConfig();
            log.info("Will upload to {}:{}", config.getUploadToHost(), config.getUploadToPath());
        } catch (IOException e) {
            throw new FileUploadException(String.format("Failed to validate %s:%s", config.getUploadToHost(), config.getUploadToPath()), e);
        }
    }

    @Override
    public void uploadFile(File file) throws FileUploadException {
        if (config.isUploadEnabled()) {
            Instant startedAt = now();
            log.info("Uploading {} to {}:{}...", file.getName(), config.getUploadToHost(), config.getUploadToPath());

            try {
                SSHClient sshClient = createAuthenticatedSshClient();

                doUploadFile(sshClient, file);
                log.info("{} ({}) uploaded to {}:{} in {} s", file.getName(), LogUtil.humanReadableByteCount(file.length()),
                         config.getUploadToHost(), config.getUploadToPath(), Duration.between(startedAt, now()).getSeconds());
            } catch (IOException e) {
                throw new FileUploadException("Cannot upload " + file, e);
            }
        }
    }

    private void doUploadFile(SSHClient sshClient, File file) throws IOException {
        try {
            String realTarget = getUploadTargetFile(file.getName());
            String tmpTarget = realTarget + "." + UUID.randomUUID();

            sshClient.newSCPFileTransfer().upload(file.getAbsolutePath(), tmpTarget);

            renameFile(sshClient, tmpTarget, realTarget);
        } finally {
            sshClient.disconnect();
        }
    }

    private String getUploadTargetFile(String fileName) {
        StringBuilder sb = new StringBuilder(config.getUploadToPath());
        if (!config.getUploadToPath().endsWith(File.separator)) {
            sb.append(File.separator);
        }
        sb.append(fileName);
        return sb.toString();
    }

    private void renameFile(SSHClient sshClient, String from, String to) throws ConnectionException, TransportException {
        try (Session session = sshClient.startSession()) {

            Session.Command command = session.exec(String.format("mv --force %s %s", from, to));

            command.join(10, TimeUnit.SECONDS);

            String errorMessage = ofNullable(command.getExitErrorMessage()).orElse("");
            int exitStatus = ofNullable(command.getExitStatus()).orElse(0);

            if (exitStatus != 0 || !errorMessage.isEmpty()) {
                log.error("Could not rename uploaded file: [{}] {}", exitStatus, errorMessage);
            }
        }
    }

    private void doValidateUploadConfig() throws IOException {
        String tmpFile = getUploadTargetFile(InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString() + ".tmp");
        String remoteCommand = String.format("mkdir -p %1$s && touch %2$s && rm %2$s", config.getUploadToPath(), tmpFile);
        log.debug("Validating upload config by attempting to execute '{}' on {}...", remoteCommand, config.getUploadToHost());

        SSHClient sshClient = createAuthenticatedSshClient();

        try (Session session = sshClient.startSession()) {
            Session.Command command = session.exec(remoteCommand);
            command.join(10, TimeUnit.SECONDS);

            String errorMessage = ofNullable(command.getExitErrorMessage()).orElse("");
            int exitStatus = ofNullable(command.getExitStatus()).orElse(0);

            if (exitStatus != 0 || !errorMessage.isEmpty()) {
                if (!errorMessage.isEmpty()) {
                    errorMessage = ": " + errorMessage;
                }
                throw new IOException(String.format("Failed to execute '%s': [exitCode=%d]%s", remoteCommand, exitStatus, errorMessage));
            }
        } finally {
            sshClient.disconnect();
        }
    }

    @Nonnull
    private SSHClient createAuthenticatedSshClient() throws IOException {
        SSHClient sshClient = new SSHClient();

        if (config.isVerifyUploadToHostKey()) {
            // Let SSH validate against ~/.ssh/known_hosts
            sshClient.loadKnownHosts();
        } else {
            // Just compare the hostnames...
            sshClient.addHostKeyVerifier((hostname, port, key) -> {
                boolean trusted = config.getUploadToHost().equals(hostname);
                if (trusted) {
                    log.info("Trusting {}:{}", hostname, port);
                } else {
                    log.error("Not trusting {}:{}, the configured uploadToHost is {}", hostname, port, config.getUploadToHost());
                }
                return trusted;
            });
        }
        sshClient.connect(config.getUploadToHost());
        sshClient.authPublickey(System.getProperty("user.name"));
        return sshClient;
    }

}
