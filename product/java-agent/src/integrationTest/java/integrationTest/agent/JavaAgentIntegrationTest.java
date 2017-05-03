package integrationTest.agent;

import io.codekvast.javaagent.AspectjMessageHandler;
import io.codekvast.testsupport.ProcessUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
@RunWith(MockitoJUnitRunner.class)
public class JavaAgentIntegrationTest {

    private final String aspectjWeaver = System.getProperty("integrationTest.aspectjWeaver");
    private final String jacocoAgent = System.getProperty("integrationTest.jacocoAgent");
    private final String codekvastAgent = System.getProperty("integrationTest.codekvastAgent");
    private final String classpath = System.getProperty("integrationTest.classpath");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void should_have_paths_to_javaagents() throws Exception {
        // given

        // when

        // then
        assertThat(aspectjWeaver, notNullValue());
        assertThat(jacocoAgent, notNullValue());
        assertThat(codekvastAgent, notNullValue());
        assertThat(classpath, notNullValue());
    }

    @Test
    public void should_not_start_when_no_config() throws Exception {
        // given
        List<String> command = buildJavaCommand(null);

        // when
        String result = ProcessUtils.executeCommand(command);

        // then
        assertThat(result, containsString("No configuration file found, Codekvast will not start"));
    }

    @Test
    @Ignore("TODO: rewrite test")
    public void should_collect_data_when_valid_config_specified() throws Exception {
        // given
        List<String> command = buildJavaCommand("build/resources/integrationTest/codekvast.conf");

        // when
        String stdout = ProcessUtils.executeCommand(command);

        // then
        assertThat(stdout, containsString("Found " + temporaryFolder.getRoot().getAbsolutePath() + "/codekvast.conf"));
        assertThat(stdout, not(containsString("SLF4J: Defaulting to no-operation (NOP) logger implementation")));
        assertThat(stdout, containsString("INFO " + AspectjMessageHandler.LOGGER_NAME));
        assertThat(stdout, containsString("AspectJ Weaver Version"));
        assertThat(stdout, containsString("Join point 'method-execution(void sample.app.SampleApp.main(java.lang.String[]))"));
        assertThat(stdout, containsString("[main] INFO sample.app.SampleApp - 2+2=4"));
        assertThat(stdout, containsString("[Codekvast Shutdown Hook]"));

        // TODO verify publications
    }

    private List<String> buildJavaCommand(String configPath) {
        List<String> command = new ArrayList<>(Arrays.asList("java",
                                                             "-javaagent:" + jacocoAgent,
                                                             "-javaagent:" + codekvastAgent,
                                                             "-javaagent:" + aspectjWeaver,
                                                             "-cp", classpath));
        if (configPath != null) {
            command.add("-Dcodekvast.configuration=" + configPath);
        }
        command.add("sample.app.SampleApp");
        return command;
    }

}
