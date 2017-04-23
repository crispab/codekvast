package integrationTest.collector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import io.codekvast.agent.collector.AspectjMessageHandler;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import io.codekvast.agent.lib.util.FileUtils;
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
public class CollectorIntegrationTest {

    private final String aspectjweaver = System.getProperty("integrationTest.aspectjweaver");
    private final String jacocoagent = System.getProperty("integrationTest.jacocoagent");
    private final String codekvastCollector = System.getProperty("integrationTest.codekvastCollector");
    private final String classpath = System.getProperty("integrationTest.classpath");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CollectorConfig collectorConfig;

    private final Map<String, File> collectorOutputFiles = new TreeMap<String, File>();

    @Before
    public void beforeTest() throws Exception {
        collectorConfig = CollectorConfigFactory.createSampleCollectorConfig().toBuilder()
                                                .appName("SampleApp")
                                                .aspectjOptions("-verbose -showWeaveInfo")
                                                .packages("sample")
                                                .dataPath(temporaryFolder.newFolder())
                                                .build();
    }

    @Test
    public void should_have_paths_to_javaagents() throws Exception {
        // given

        // when

        // then
        assertThat(aspectjweaver, notNullValue());
        assertThat(jacocoagent, notNullValue());
        assertThat(codekvastCollector, notNullValue());
        assertThat(classpath, notNullValue());
    }

    @Test
    public void should_not_start_collector_when_no_config() throws Exception {
        // given
        List<String> command = buildJavaCommand(null);

        // when
        String result = ProcessUtils.executeCommand(command);

        // then
        assertThat(result, containsString("No configuration file found, Codekvast will not start"));
    }

    @Test
    public void should_collect_data_when_valid_config_specified() throws Exception {
        // given
        List<String> command = buildJavaCommand(writeCollectorConfigToFile());

        // when
        String stdout = ProcessUtils.executeCommand(command);

        // then
        assertThat(stdout, containsString("Found " + temporaryFolder.getRoot().getAbsolutePath() + "/codekvast.conf"));
        assertThat(stdout, not(containsString("SLF4J: Defaulting to no-operation (NOP) logger implementation")));
        assertThat(stdout, containsString("INFO " + AspectjMessageHandler.LOGGER_NAME));
        assertThat(stdout, containsString("AspectJ Weaver Version"));
        assertThat(stdout, containsString("Join point 'method-execution(void sample.app.SampleApp.main(java.lang.String[]))"));
        assertThat(stdout, containsString("[main] INFO sample.app.SampleApp - 2+2=4"));

        walkFileTree(collectorOutputFiles, collectorConfig.getDataPath());

        assertThat(collectorOutputFiles.keySet(), hasItems("aop.xml", "invocations.dat.00000", "jvm.dat"));

        List<String> lines = readLinesFrom("invocations.dat.00000");
        assertThat(lines, hasItem("public sample.app.SampleApp.main(java.lang.String[])"));
        assertThat(lines, hasItem("public sample.app.SampleApp.add(int, int)"));
        assertThat(lines, hasItem("public sample.app.SampleApp()"));
        assertThat(lines.size(), is(6));
    }

    private List<String> readLinesFrom(String basename) throws IOException {
        File file = collectorOutputFiles.get(basename);
        List<String> result = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }

    private String writeCollectorConfigToFile() throws IOException {
        File file = new File(temporaryFolder.getRoot(), "codekvast.conf");
        FileUtils.writePropertiesTo(file, collectorConfig, getClass().getSimpleName());
        return file.getAbsolutePath();
    }

    private List<String> buildJavaCommand(String configPath) {
        List<String> command = new ArrayList<String>(Arrays.asList("java",
                                                                   "-javaagent:" + jacocoagent,
                                                                   "-javaagent:" + codekvastCollector,
                                                                   "-javaagent:" + aspectjweaver,
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
