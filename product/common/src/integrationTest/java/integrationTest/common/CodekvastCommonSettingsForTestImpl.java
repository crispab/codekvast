package integrationTest.common;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import lombok.Data;

/**
 * @author olle.hallin@crisp.se
 */
@Data
class CodekvastCommonSettingsForTestImpl implements CodekvastCommonSettings {

    private String applicationName;

    private String displayVersion;

    private String dnsCname;

    private String slackWebHookToken;

    private String slackWebHookUrl;

    private String herokuCodekvastUrl;

    private String herokuApiPassword;
}
