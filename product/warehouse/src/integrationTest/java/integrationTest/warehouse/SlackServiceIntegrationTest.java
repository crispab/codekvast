package integrationTest.warehouse;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.messaging.SlackService;
import io.codekvast.warehouse.messaging.impl.SlackServiceImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
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
    public void should_have_wired_context_correctly() throws Exception {
        assertThat(slackService, not(nullValue()));
        assertThat(settings, not(nullValue()));
        assertThat(settings.getSlackWebHookUrl().isEmpty(), is(false));
        assertThat(settings.getSlackWebHookToken().isEmpty(), is(false));
    }

    @Test
    public void slack_service_should_be_enabled() throws Exception {
        assertThat(slackService.isEnabled(), is(true));
    }

    @Test
    @Ignore("Sends stuff to codekvast.slack.com")
    public void should_send_to_slack() throws Exception {
        slackService.sendNotification("_Hello, World!_ says `" + getClass().getName() + "`", "integration-test");
    }
}