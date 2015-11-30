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
