package integrationTest.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.messaging.SlackService;
import io.codekvast.common.messaging.impl.SlackServiceImpl;
import io.codekvast.common.metrics.CommonMetricsService;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

/**
 * A manual integration test.
 *
 * <p>It is normally disabled, since we don't want to install the Slack webhook integration token
 * into Jenkins.
 *
 * @author olle.hallin@crisp.se
 */
public class SlackServiceIntegrationTest {

  private final CodekvastCommonSettings settings = new CodekvastCommonSettings();

  private SlackService slackService;

  @BeforeEach
  public void beforeTest() throws Exception {
    assumeTrue("true".equals(System.getenv("RUN_SLACK_TESTS")));

    MockitoAnnotations.initMocks(this);

    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/secrets.properties"));

    settings.setSlackWebHookToken(props.getProperty("codekvast.slackWebHookToken"));
    slackService = new SlackServiceImpl(settings, mock(CommonMetricsService.class));
  }

  @Test
  public void should_have_wired_context_correctly() {
    assertThat(slackService, not(nullValue()));
    assertThat(settings, not(nullValue()));
    assertThat(settings.getSlackWebHookUrl().isEmpty(), is(false));
    assertThat(settings.getSlackWebHookToken().isEmpty(), is(false));
  }

  @Test
  public void should_send_to_slack() {
    slackService.sendNotification(
        "`" + getClass().getName() + "` says _Hello, World!_", SlackService.Channel.BUILDS);
  }
}
