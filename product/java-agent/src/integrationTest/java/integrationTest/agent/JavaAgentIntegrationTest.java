package integrationTest.agent;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import io.codekvast.javaagent.AspectjMessageHandler;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.util.FileUtils;
import io.codekvast.testsupport.ProcessUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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

    private AgentConfig agentConfig;

    private final Map<String, File> agentOutputFiles = new TreeMap<>();

    @Before
    public void beforeTest() throws Exception {
        agentConfig = AgentConfigFactory.createSampleAgentConfig().toBuilder()
                                        .appName("SampleApp")
                                        .aspectjOptions("-verbose -showWeaveInfo")
                                        .packages("sample")
                                        .dataPath(temporaryFolder.newFolder())
                                        .licenseKey("licenseKey")
                                        .build();
    }

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
        // TODO Rewrite test

        // given
        List<String> command = buildJavaCommand(writeAgentConfigToFile());

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

        walkFileTree(agentOutputFiles, agentConfig.getDataPath());

        assertThat(agentOutputFiles.keySet(), hasItems("aop.xml", "invocations.dat.00000", "jvm.dat"));

        List<String> lines = readLinesFrom("invocations.dat.00000");
        assertThat(lines, hasItem("public sample.app.SampleApp.main(java.lang.String[])"));
        assertThat(lines, hasItem("public sample.app.SampleApp.add(int, int)"));
        assertThat(lines, hasItem("public sample.app.SampleApp()"));
        assertThat(lines.size(), is(6));
    }

    private List<String> readLinesFrom(String basename) throws IOException {
        File file = agentOutputFiles.get(basename);
        List<String> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }

    private String writeAgentConfigToFile() throws IOException {
        File file = new File(temporaryFolder.getRoot(), "codekvast.conf");
        FileUtils.writePropertiesTo(file, agentConfig, getClass().getSimpleName());
        return file.getAbsolutePath();
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

    private void walkFileTree(Map<String, File> result, File path) {
        for (File file : path.listFiles()) {
            if (file.isDirectory()) {
                walkFileTree(result, file);
            } else {
                result.put(file.getName(), file);
            }
        }
    }
}
