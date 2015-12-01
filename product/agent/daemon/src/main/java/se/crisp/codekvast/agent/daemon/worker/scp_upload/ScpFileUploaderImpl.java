/**
 * Copyright (c) 2015 Crisp AB
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
package se.crisp.codekvast.agent.daemon.worker.scp_upload;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.util.LogUtil;
import se.crisp.codekvast.agent.daemon.worker.FileUploadException;
import se.crisp.codekvast.agent.daemon.worker.FileUploader;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

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
        if (config.isUploadEnabled()) {
            log.info("Will upload to {}:{}", config.getUploadToHost(), config.getUploadToPath());
        } else {
            log.debug("Will not upload");
        }
    }

    @Override
    public void uploadFile(File file) throws FileUploadException {
        if (config.isUploadEnabled()) {
            Instant startedAt = now();
            log.info("Uploading {} to {}:{}...", file.getName(), config.getUploadToHost(), config.getUploadToPath());

            SSHClient ssh = new SSHClient();
            try {
                doUploadFile(ssh, file);
                log.info("{} ({}) uploaded to {}:{} in {} s", file.getName(), LogUtil.humanReadableByteCount(file.length()),
                         config.getUploadToHost(), config.getUploadToPath(), Duration.between(startedAt, now()).getSeconds());
            } catch (IOException e) {
                throw new FileUploadException("Cannot upload " + file, e);
            }
        }
    }

    private void doUploadFile(SSHClient ssh, File file) throws IOException {
        try {
            ssh.loadKnownHosts();
            ssh.connect(config.getUploadToHost());
            ssh.authPublickey(System.getProperty("user.name"));
            ssh.newSCPFileTransfer().upload(file.getAbsolutePath(), config.getUploadToPath());
        } finally {
            ssh.disconnect();
        }
    }
}
