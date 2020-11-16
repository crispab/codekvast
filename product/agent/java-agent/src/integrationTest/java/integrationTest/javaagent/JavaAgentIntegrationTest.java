package integrationTest.javaagent;

import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.codekvast.javaagent.model.Endpoints.Agent.V2_POLL_CONFIG;
import static io.codekvast.javaagent.model.Endpoints.Agent.V2_UPLOAD_INVOCATION_DATA;
import static io.codekvast.javaagent.model.Endpoints.Agent.V3_UPLOAD_CODEBASE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import io.codekvast.javaagent.AspectjMessageHandler;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v2.GetConfigResponse2;
import io.codekvast.javaagent.util.FileUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class JavaAgentIntegrationTest {

  private static final String jacocoAgentPath = System.getProperty("integrationTest.jacocoAgent");
  private static final String codekvastAgentPath =
      System.getProperty("integrationTest.codekvastAgent");
  private static final String classpath = System.getProperty("integrationTest.classpath");
  private static final String javaVersionsString =
      System.getProperty("integrationTest.javaVersions");
  private static final Gson gson = new Gson();
  private static WireMockServer wireMockServer;

  @BeforeAll
  public static void beforeAll() {
    assertNotNull(jacocoAgentPath, "This test must be started from Gradle");
    assertNotNull(codekvastAgentPath, "This test must be started from Gradle");
    assertNotNull(classpath, "This test must be started from Gradle");
    assertNotNull(javaVersionsString, "This test must be started from Gradle");

    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    WireMock.configureFor(wireMockServer.port());
  }

  @AfterAll
  public static void afterAll() {
    wireMockServer.shutdown();
  }

  @Test
  public void should_not_start_when_no_config() throws Exception {
    // given
    List<String> command = buildJavaCommand(getLowestJavaVersion(), null);

    // when
    String stdout = executeCommand(command);

    // then
    assertThat(stdout, containsString("No configuration file found, Codekvast will not start"));
    assertSampleAppOutput(stdout);
  }

  @Test
  public void should_not_start_when_not_found_config() throws Exception {
    // given
    List<String> command = buildJavaCommand(getLowestJavaVersion(), "foobar");

    // when
    String stdout = executeCommand(command);

    // then
    assertThat(stdout, containsString("Trying foobar"));
    assertThat(
        stdout,
        containsString("Invalid value of -Dcodekvast.configuration or CODEKVAST_CONFIG: foobar"));
    assertThat(stdout, containsString("Codekvast will not start"));
    assertSampleAppOutput(stdout);
  }

  @Test
  public void should_not_start_when_disabled_in_config() throws Exception {
    // given
    List<String> command =
        buildJavaCommand(getLowestJavaVersion(), createAgentConfigFile(false).getAbsolutePath());

    // when
    String stdout = executeCommand(command);

    // then
    assertThat(stdout, containsString("Codekvast is disabled"));
    assertSampleAppOutput(stdout);
  }

  @ParameterizedTest(
      name = "should weave and call server when Java version is {0} and enabled is {1}")
  @MethodSource("testConfigurations")
  public void should_weave_and_call_server(String javaVersion, boolean enabledByServer)
      throws Exception {
    // given

    givenThat(
        post(V2_POLL_CONFIG)
            .willReturn(
                okJson(
                    gson.toJson(
                        GetConfigResponse2.builder()
                            .codeBasePublisherName("http")
                            .codeBasePublisherConfig("enabled=" + enabledByServer)
                            .customerId(1L)
                            .invocationDataPublisherName("http")
                            .invocationDataPublisherConfig("enabled=" + enabledByServer)
                            .configPollIntervalSeconds(1)
                            .configPollRetryIntervalSeconds(1)
                            .codeBasePublisherCheckIntervalSeconds(1)
                            .codeBasePublisherRetryIntervalSeconds(1)
                            .invocationDataPublisherIntervalSeconds(1)
                            .invocationDataPublisherRetryIntervalSeconds(1)
                            .build()))));

    givenThat(post(V3_UPLOAD_CODEBASE).willReturn(ok()));
    givenThat(post(V2_UPLOAD_INVOCATION_DATA).willReturn(ok()));

    File agentConfigFile = createAgentConfigFile(true);

    List<String> command = buildJavaCommand(javaVersion, agentConfigFile.getAbsolutePath());

    // when
    String stdout = executeCommand(command);
    System.out.printf(
        "stdout from the JVM is%n--------------------------------------------------%n%s%n--------------------------------------------------%n%n",
        stdout);

    // then
    assertThat(stdout, containsString("Found " + agentConfigFile.getAbsolutePath()));
    assertThat(stdout, containsString("[INFO] " + AspectjMessageHandler.LOGGER_NAME));
    assertThat(stdout, containsString("AspectJ Weaver Version "));
    assertThat(
        stdout, containsString("define aspect io.codekvast.javaagent.MethodExecutionAspect"));
    assertThat(
        stdout,
        containsString("Join point 'constructor-execution(void sample.app.SampleApp.<init>("));
    assertThat(
        stdout,
        containsString("Join point 'method-execution(int sample.app.SampleApp.add(int, int))'"));
    assertThat(
        stdout,
        containsString(
            "Join point 'method-execution(void sample.app.SampleApp.main(java.lang.String[]))'"));
    assertThat(
        stdout,
        not(
            containsString(
                "Join point 'method-execution(void sample.app.SampleAspect.logAspectLoaded())'")));
    assertThat(
        stdout,
        not(
            containsString(
                "Join point 'method-execution(int sample.app.SampleApp.privateAdd(int, int))'")));
    assertThat(
        stdout,
        not(
            containsString(
                "Join point 'method-execution(void sample.app.excluded.NotTrackedClass.doSomething())'")));
    assertThat(stdout, containsString("Codekvast shutdown completed in "));
    assertThat(stdout, not(containsString("error")));
    assertThat(stdout, not(containsString("[SEVERE]")));
    if (atLeastJava9(javaVersion)) {
      assertThat(
          stdout,
          containsString(
              "no longer creating weavers for these classloaders: [jdk.internal.loader.ClassLoaders$PlatformClassLoader]"));
    }
    assertSampleAppOutput(stdout);

    verify(postRequestedFor(urlEqualTo(V2_POLL_CONFIG)));

    if (enabledByServer) {
      verify(postRequestedFor(urlEqualTo(V3_UPLOAD_CODEBASE)));
      verify(postRequestedFor(urlEqualTo(V2_UPLOAD_INVOCATION_DATA)));
    }
  }

  private static List<String> getJavaVersions() {
    return Arrays.stream(javaVersionsString.split(","))
        .map(String::trim)
        .collect(Collectors.toList());
  }

  private static String getLowestJavaVersion() {
    List<String> versions = getJavaVersions();
    return versions.get(versions.size() - 1);
  }

  private static String getHighestJavaVersion() {
    List<String> versions = getJavaVersions();
    return versions.get(0);
  }

  public static List<Arguments> testConfigurations() {
    List<Arguments> result = new ArrayList<>();
    result.add(Arguments.of(getHighestJavaVersion(), false));
    for (String v : getJavaVersions()) {
      result.add(Arguments.of(v, true));
    }
    result.add(Arguments.of(getLowestJavaVersion(), false));
    return result;
  }

  private void assertSampleAppOutput(String stdout) {
    assertThat(stdout, containsString("[INFO] sample.app.SampleApp - SampleApp starts"));
    assertThat(stdout, containsString("[INFO] sample.app.SampleApp - 2+2=4"));
    assertThat(
        stdout,
        containsString(
            "[INFO] sample.app.SampleAspect - Before execution(void sample.app.SampleService1.doSomething(int))"));
    assertThat(stdout, containsString("[INFO] sample.app.SampleService1 - Doing something 1"));
    assertThat(
        stdout,
        containsString(
            "[INFO] sample.app.SampleAspect - After execution(void sample.app.SampleService1.doSomething(int))"));
    assertThat(stdout, containsString("[INFO] sample.app.SampleApp - Exit"));
  }

  private File createAgentConfigFile(boolean enabled) throws Exception {
    AgentConfig agentConfig =
        AgentConfigFactory.createTemplateConfig().toBuilder()
            .serverUrl("http://localhost:" + wireMockServer.port())
            .appName("SampleApp")
            .appVersion("literal 1.0")
            .aspectjOptions("-verbose -showWeaveInfo")
            .enabled(enabled)
            .packages("sample")
            .methodVisibility("protected")
            .excludePackages("sample.app.excluded")
            .codeBase("build/classes/java/integrationTest")
            .bridgeAspectjMessagesToJUL(true)
            .schedulerInitialDelayMillis(0)
            .schedulerIntervalMillis(100)
            .build();
    File agentConfigFile = FileUtils.serializeToFile(agentConfig, "codekvast", ".conf.ser");
    agentConfigFile.deleteOnExit();
    return agentConfigFile;
  }

  private boolean atLeastJava9(String javaVersion) {
    return !javaVersion.startsWith("8");
  }

  private List<String> buildJavaCommand(String javaVersion, String configPath) {
    String cp =
        classpath.endsWith(":") ? classpath.substring(0, classpath.length() - 2) : classpath;

    String java =
        String.format("%s/.sdkman/candidates/java/%s/bin/java", System.getenv("HOME"), javaVersion);

    List<String> command =
        new ArrayList<>(
            Arrays.asList(
                java,
                // TODO: Make integration test work with JaCoCo: "-javaagent:" + jacocoAgent,
                "-javaagent:" + codekvastAgentPath,
                "-cp",
                cp,
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

  private String executeCommand(List<String> command)
      throws RuntimeException, IOException, InterruptedException {
    Process process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
    int exitCode = process.waitFor();
    String output = collectProcessOutput(process.getInputStream());
    if (exitCode != 0) {
      throw new RuntimeException(
          String.format("Could not execute '%s': %s%nExit code=%d", command, output, exitCode));
    }

    return output;
  }

  private String collectProcessOutput(InputStream inputStream) throws IOException {
    StringBuilder sb = new StringBuilder();
    String line;
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String newLine = "";
    while ((line = reader.readLine()) != null) {
      sb.append(newLine).append(line);
      newLine = String.format("%n");
    }
    return sb.toString();
  }
}
