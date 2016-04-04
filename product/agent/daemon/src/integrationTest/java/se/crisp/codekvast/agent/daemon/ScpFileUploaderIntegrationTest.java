package se.crisp.codekvast.agent.daemon;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.worker.FileUploadException;
import se.crisp.codekvast.agent.daemon.worker.impl.ScpFileUploaderImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeThat;

/**
 * @author olle.hallin@crisp.se
 */
public class ScpFileUploaderIntegrationTest {

    private static String sshHost = null;
    private static Integer sshPort = null;
    private static String containerId;

    private ScpFileUploaderImpl scpUploader;

    @BeforeClass
    public static void startDockerImage() {
        String imageName = "rastasheep/ubuntu-sshd:14.04";
        try {
            Process process = Runtime.getRuntime().exec("docker run -d -P " + imageName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            containerId = reader.readLine();
            process.waitFor();

            process = Runtime.getRuntime().exec("docker port " + containerId + " 22");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String[] parts = reader.readLine().split(":");
            process.waitFor();

            sshHost = parts[0];
            sshPort = Integer.valueOf(parts[1]);
        } catch (Exception e) {
            System.out.println("Cannot start docker container " + imageName + ", SCP tests will be disabled.");
        }
    }

    @AfterClass
    public static void stopDockerImage() throws IOException, InterruptedException {
        if (containerId != null) {
            Runtime.getRuntime().exec("docker stop " + containerId).waitFor();
            Runtime.getRuntime().exec("docker rm " + containerId).waitFor();
        }
    }

    @Before
    public void beforeTest() throws Exception {
        assumeThat(sshPort, not(nullValue()));

        DaemonConfig config = DaemonConfig.builder()
                                          .uploadToHost(sshHost)
                                          .uploadToPort(sshPort)
                                          .uploadToUsername("root")
                                          .uploadToPassword("root")
                                          .uploadToPath("/tmp")
                                          .verifyUploadToHostKey(false)

                                          .dataPath(new File("dataPath"))
                                          .dataProcessingIntervalSeconds(10)
                                          .daemonVersion("version")
                                          .daemonVcsId("vcsId")
                                          .environment("integration-test")
                                          .exportFile(new File("exportFile"))
                                          .build();

        scpUploader = new ScpFileUploaderImpl(config);
    }

    @Test
    public void should_validate_ssh() throws FileUploadException {
        assumeThat(sshPort, not(nullValue()));

        scpUploader.validateUploadConfig();
    }

    @Test
    public void should_upload_file() throws FileUploadException, URISyntaxException {
        assumeThat(sshPort, not(nullValue()));

        scpUploader.uploadFile(new File(getClass().getClassLoader().getResource("scpUploadTest.txt").toURI()));
    }
}
