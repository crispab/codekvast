package integrationTest.common;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.messaging.SlackService;
import io.codekvast.common.messaging.impl.SlackServiceImpl;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;

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

    private SlackService slackService;
    private CodekvastCommonSettingsForTestImpl settings = new CodekvastCommonSettingsForTestImpl();

    @Before
    public void beforeTest() throws Exception {
        assumeTrue("true".equals(System.getenv("RUN_SLACK_TESTS")));

        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/secrets.properties"));

        settings.setSlackWebHookUrl("https://hooks.slack.com/services");
        settings.setSlackWebHookToken(props.getProperty("codekvast.slackWebHookToken"));
        slackService = new SlackServiceImpl(settings);
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

    @Data
    private static class CodekvastCommonSettingsForTestImpl implements CodekvastCommonSettings {
        private String applicationName;
        private String displayVersion;
        private String dnsCname;
        private String herokuApiPassword;
        private String herokuApiSsoSalt;
        private String herokuCodekvastUrl;
        private String slackWebHookToken;
        private String slackWebHookUrl;
        private String dashboardJwtSecret;
        private Long dashboardJwtExpirationHours;
    }
}