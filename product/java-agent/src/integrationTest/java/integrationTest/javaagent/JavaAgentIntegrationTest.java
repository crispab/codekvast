package integrationTest.javaagent;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import io.codekvast.javaagent.AspectjMessageHandler;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.util.FileUtils;
import io.codekvast.testsupport.ProcessUtils;
import lombok.RequiredArgsConstructor;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class JavaAgentIntegrationTest {

    // TODO: private final String jacocoAgent = System.getProperty("integrationTest.jacocoAgent");
    private static final String codekvastAgent = System.getProperty("integrationTest.codekvastAgent");
    private static final String classpath = System.getProperty("integrationTest.classpath");
    private static final String javaVersions = System.getProperty("integrationTest.javaVersions");

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    private static final Gson gson = new Gson();

    private static File agentConfigFile;

    @Parameterized.Parameters(name = "JVM version = {0}, agent enabled={1}")
    public static List<Object[]> testParameters() {
        // The streams API is not available in Java 7!
        List<Object[]> result = new ArrayList<>();
        for (String version : javaVersions.split(",")) {
            result.add(new Object[]{version.trim(), false});
            result.add(new Object[]{version.trim(), true});
        }
        return result;
    }

    // Is injected from testParameters()
    private final String javaVersion;

    private final Boolean agentEnabled;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // TODO: assertThat(jacocoAgent, notNullValue());
        assertThat(codekvastAgent, notNullValue());
        assertThat(classpath, notNullValue());
        assertThat(javaVersions, notNullValue());

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
    public void should_not_start_when_no_config() throws Exception {
        // given
        List<String> command = buildJavaCommand(null);

        // when
        String result = ProcessUtils.executeCommand(command);

        // then
        assertThat(result, containsString("No configuration file found, Codekvast will not start"));
    }

    @Test
    public void should_weave_and_call_server() throws Exception {
        // given
        givenThat(post(V1_POLL_CONFIG)
                      .willReturn(okJson(gson.toJson(
                          GetConfigResponse1.builder()
                                            .codeBasePublisherName("http")
                                            .codeBasePublisherConfig("enabled=" + agentEnabled)
                                            .customerId(1L)
                                            .invocationDataPublisherName("http")
                                            .invocationDataPublisherConfig("enabled=" + agentEnabled)
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
        System.out.printf("stdout from the JVM is%n--------------------------------------------------%n%s%n--------------------------------------------------%n%n", stdout);

        // then
        assertThat(stdout, containsString("Found " + agentConfigFile.getAbsolutePath()));
        assertThat(stdout, containsString("[INFO] " + AspectjMessageHandler.LOGGER_NAME));
        assertThat(stdout, containsString("AspectJ Weaver Version "));
        assertThat(stdout, containsString("no longer creating weavers for these classloaders"));
        assertThat(stdout, containsString("[INFO] sample.app.SampleApp - 2+2=4"));
        assertThat(stdout, containsString("define aspect io.codekvast.javaagent.MethodExecutionAspect"));
        assertThat(stdout, containsString("Join point 'method-execution(int sample.app.SampleApp.add(int, int))'"));
        assertThat(stdout, containsString("Join point 'method-execution(void sample.app.SampleApp.main(java.lang.String[]))'"));
        assertThat(stdout, containsString("Codekvast shutdown completed in "));
        assertThat(stdout, not(containsString("error")));
        assertThat(stdout, not(containsString("[SEVERE]")));

        verify(postRequestedFor(urlEqualTo(V1_POLL_CONFIG)));

        if (agentEnabled) {
            verify(postRequestedFor(urlEqualTo(V2_UPLOAD_CODEBASE)));
            verify(postRequestedFor(urlEqualTo(V2_UPLOAD_INVOCATION_DATA)));
        }

    }

    private List<String> buildJavaCommand(String configPath) {
        String cp = classpath.endsWith(":") ? classpath.substring(0, classpath.length()-2) : classpath;

        String java = String.format("%s/.sdkman/candidates/java/%s/bin/java", System.getenv("HOME"), javaVersion);

        List<String> command = new ArrayList<>(
            Arrays.asList(java,
                          // TODO: "-javaagent:" + jacocoAgent,
                          "-javaagent:" + codekvastAgent,
                          "-cp", cp,
                          "-Djava.util.logging.config.file=src/integrationTest/resources/logging.properties",
                          "-Duser.language=en",
                          "-Duser.country=US"));
        if (configPath != null) {
            command.add("-Dcodekvast.configuration=" + configPath);
        }
        command.add("sample.app.SampleApp");
        System.out.printf("%nLaunching SampleApp with the command: %s%n%n", command);
        return command;
    }

}
