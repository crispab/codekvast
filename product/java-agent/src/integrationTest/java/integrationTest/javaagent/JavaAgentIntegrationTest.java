package integrationTest.javaagent;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import io.codekvast.javaagent.AspectjMessageHandler;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.util.FileUtils;
import io.codekvast.testsupport.ProcessUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.codekvast.javaagent.model.Endpoints.Agent.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
@RunWith(MockitoJUnitRunner.class)
public class JavaAgentIntegrationTest {

    private final String jacocoAgent = System.getProperty("integrationTest.jacocoAgent");
    private final String codekvastAgent = System.getProperty("integrationTest.codekvastAgent");
    private final String classpath = System.getProperty("integrationTest.classpath");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    private final Gson gson = new Gson();

    private File agentConfigFile;

    @Before
    public void setUp() throws Exception {
        AgentConfig agentConfig = AgentConfigFactory.createTemplateConfig().toBuilder()
                                                    .serverUrl("http://localhost:" + wireMockRule.port())
                                                    .appName("SampleApp")
                                                    .appVersion("literal 1.0")
                                                    .aspectjOptions("-verbose -showWeaveInfo")
                                                    .packages("sample")
                                                    .codeBase("build/classes/java/integrationTest")
                                                    .bridgeAspectjMessagesToJUL(true)
                                                    .schedulerInitialDelayMillis(0)
                                                    .schedulerIntervalMillis(100)
                                                    .build();
        agentConfigFile = FileUtils.serializeToFile(agentConfig, "codekvast", ".conf.ser");
        agentConfigFile.deleteOnExit();
    }

    @Test
    public void should_have_been_invoked_correctly() {
        // given

        // when

        // then
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
    public void should_collect_data_when_valid_config_specified() throws Exception {
        // given
        givenThat(post(V1_POLL_CONFIG)
                      .willReturn(okJson(gson.toJson(
                          GetConfigResponse1.builder()
                                            .codeBasePublisherName("http")
                                            .codeBasePublisherConfig("enabled=true")
                                            .customerId(1L)
                                            .invocationDataPublisherName("http")
                                            .invocationDataPublisherConfig("enabled=true")
                                            .configPollIntervalSeconds(1)
                                            .configPollRetryIntervalSeconds(1)
                                            .codeBasePublisherCheckIntervalSeconds(1)
                                            .codeBasePublisherRetryIntervalSeconds(1)
                                            .invocationDataPublisherIntervalSeconds(1)
                                            .invocationDataPublisherRetryIntervalSeconds(1)
                                            .build()))));

        givenThat(post(V2_UPLOAD_CODEBASE).willReturn(ok()));
        givenThat(post(V2_UPLOAD_INVOCATION_DATA).willReturn(ok()));

        List<String> command = buildJavaCommand(agentConfigFile.getAbsolutePath());

        // when
        String stdout = ProcessUtils.executeCommand(command);
        System.out.printf("stdout = %n%s%n", stdout);

        // then
        assertThat(stdout, containsString("Found " + agentConfigFile.getAbsolutePath()));
        assertThat(stdout, containsString("[INFO] " + AspectjMessageHandler.LOGGER_NAME));
        assertThat(stdout, containsString("AspectJ Weaver Version "));
        assertThat(stdout, containsString("[INFO] sample.app.SampleApp - 2+2=4"));
        assertThat(stdout, containsString("Join point 'method-execution(void sample.app.SampleApp.main(java.lang.String[]))"));
        assertThat(stdout, containsString("Codekvast shutdown completed in "));

        verify(postRequestedFor(urlEqualTo(V1_POLL_CONFIG)));
        verify(postRequestedFor(urlEqualTo(V2_UPLOAD_CODEBASE)));
        verify(postRequestedFor(urlEqualTo(V2_UPLOAD_INVOCATION_DATA)));

        assertThat(stdout, not(containsString("error")));
        assertThat(stdout, not(containsString("[SEVERE]")));
    }

    private List<String> buildJavaCommand(String configPath) {
        String cp = classpath.endsWith(":") ? classpath.substring(0, classpath.length()-2) : classpath;
        List<String> command = new ArrayList<>(
            Arrays.asList("java",
                          "-javaagent:" + jacocoAgent,
                          "-javaagent:" + codekvastAgent,
                          "-cp", cp,
                          "-Djava.util.logging.config.file=src/integrationTest/resources/logging.properties",
                          "-Duser.language=en",
                          "-Duser.country=US"));
        if (configPath != null) {
            command.add("-Dcodekvast.configuration=" + configPath);
        }
        command.add("sample.app.SampleApp");
        return command;
    }

}
