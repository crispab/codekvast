package io.codekvast.common.bootstrap;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import lombok.Data;

/**
 * @author olle.hallin@crisp.se
 */
@Data
public class CodekvastCommonSettingsForTestImpl implements CodekvastCommonSettings {

    private String jwtSecret = "secret";
    private Long jwtExpirationHours = 1L;

    // not used in test
    private String applicationName = null;
    private String displayVersion = null;
    private String dnsCname = null;
    private String environment = null;
    private String slackWebHookToken = null;
}
