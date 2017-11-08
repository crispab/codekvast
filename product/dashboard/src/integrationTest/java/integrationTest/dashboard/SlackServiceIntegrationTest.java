package integrationTest.dashboard;

import io.codekvast.dashboard.bootstrap.CodekvastSettings;
import io.codekvast.dashboard.messaging.SlackService;
import io.codekvast.dashboard.messaging.impl.SlackServiceImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * A manual integration test.
 *
 * It is normally disabled, since we don't want to install the Slack webhook integration token into Jenkins.
 *
 * @author olle.hallin@crisp.se
 */
public class SlackServiceIntegrationTest {

    private SlackService slackService;
    private CodekvastSettings settings = new CodekvastSettings();

    @Before
    public void beforeTest() throws Exception {
        Properties props = new Properties();

        props.load(getClass().getResourceAsStream("/application.properties"));
        props.load(getClass().getResourceAsStream("/secrets.properties"));

        settings.setSlackWebHookUrl(props.getProperty("codekvast.slackWebHookUrl"));
        settings.setSlackWebHookToken(props.getProperty("codekvast.slackWebHookToken"));
        slackService = new SlackServiceImpl(settings);
    }

    @Test
    @Ignore("Requires git-crypt")
    public void should_have_wired_context_correctly() {
        assertThat(slackService, not(nullValue()));
        assertThat(settings, not(nullValue()));
        assertThat(settings.getSlackWebHookUrl().isEmpty(), is(false));
        assertThat(settings.getSlackWebHookToken().isEmpty(), is(false));
    }

    @Test
    @Ignore("Sends stuff to codekvast.slack.com")
    public void should_send_to_slack() {
        slackService.sendNotification("_Hello, World!_ says `" + getClass().getName() + "`", SlackService.Channel.BUILDS);
    }
}