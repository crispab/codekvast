package integrationTest.collector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import sample.app.SampleApp;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.util.FileUtils;
import se.crisp.codekvast.testsupport.ProcessUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        String command = buildJavaCommand(null);

        // when
        String result = ProcessUtils.executeCommand(command);

        // then
        assertThat(result, containsString("No configuration file found, Codekvast will not start"));
    }

    @Test
    public void should_collect_data_when_valid_config_specified() throws Exception {
        // given
        String command = buildJavaCommand(writeCollectorConfigToFile());

        // when
        String result = ProcessUtils.executeCommand(command);

        // then
        System.out.println("result = " + result);

        assertThat(result, containsString("Found " + temporaryFolder.getRoot().getAbsolutePath()));

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

    private String buildJavaCommand(String configPath) {
        String sysProps = configPath == null ? "" : "-Dcodekvast.configuration=" + configPath;
        return String.format(
                "java -javaagent:%s -javaagent:%s -javaagent:%s -cp %s %s %s %s",
                jacocoagent,
                codekvastCollector,
                aspectjweaver,
                classpath,
                "-Dcodekvast.options=verbose=true",
                sysProps,
                SampleApp.class.getName());
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
