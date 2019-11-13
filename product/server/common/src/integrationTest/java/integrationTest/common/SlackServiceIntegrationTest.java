package integrationTest.common;

import io.codekvast.common.bootstrap.CodekvastCommonSettingsForTestImpl;
import io.codekvast.common.messaging.SlackService;
import io.codekvast.common.messaging.impl.SlackServiceImpl;
import io.codekvast.common.metrics.CommonMetricsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * A manual integration test.
 *
 * It is normally disabled, since we don't want to install the Slack webhook integration token into Jenkins.
 *
 * @author olle.hallin@crisp.se
 */
public class SlackServiceIntegrationTest {

    @Mock
    private CommonMetricsService metricsService;

    private CodekvastCommonSettingsForTestImpl settings = new CodekvastCommonSettingsForTestImpl();

    private SlackService slackService;

    @Before
    public void beforeTest() throws Exception {
        assumeTrue("true".equals(System.getenv("RUN_SLACK_TESTS")));

        MockitoAnnotations.initMocks(this);

        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/secrets.properties"));

        settings.setSlackWebHookToken(props.getProperty("codekvast.slackWebHookToken"));
        slackService = new SlackServiceImpl(settings, metricsService);
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
        slackService.sendNotification("`" + getClass().getName() + "` says _Hello, World!_", SlackService.Channel.BUILDS);
    }

}
