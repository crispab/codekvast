package se.crisp.codekvast.agent.collector.itest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import sample.app.SampleApp;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.util.FileUtils;
import se.crisp.codekvast.testsupport.ProcessUtils;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CollectorIntegrationTest {

    private final String aspectjweaverPath = System.getProperty("codekvast.aspectjweaverPath");
    private final String collectorPath = System.getProperty("codekvast.collectorPath");
    private final String classpath = System.getProperty("codekvast.sampleAppClasspath");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void should_have_paths_to_javaagents() throws Exception {
        // given

        // when

        // then
        assertThat(aspectjweaverPath, notNullValue());
        assertThat(collectorPath, notNullValue());
        assertThat(classpath, notNullValue());
    }

    @Test
    public void should_not_start_collector_when_no_config() throws Exception {
        // given
        String command = buildJavaCommand(null);

        // when
        String result = ProcessUtils.executeCommand(command);

        // then
        assertThat(result, containsString("No configuration file found, Codekvast will not start"));
    }

    @Test
    public void should_start_collector_when_config_specified() throws Exception {
        // given
        String command = buildJavaCommand(createCollectorConfig());

        // when
        String result = ProcessUtils.executeCommand(command);

        // then
        assertThat(result, containsString("Found " + temporaryFolder.getRoot().getAbsolutePath()));
    }

    private String createCollectorConfig() throws IOException {
        CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig().toBuilder()
                                                       .dataPath(temporaryFolder.newFolder("data"))
                                                       .build();
        File file = temporaryFolder.newFile();
        FileUtils.writePropertiesTo(file, config, getClass().getSimpleName());
        return file.getAbsolutePath();
    }

    private String buildJavaCommand(String configPath) {
        String sysProps = configPath == null ? "" : "-Dcodekvast.configuration=" + configPath;
        return String.format(
                "java -javaagent:%s -javaagent:%s -cp %s %s %s",
                collectorPath,
                aspectjweaverPath,
                classpath,
                sysProps,
                SampleApp.class.getName());
    }


}
