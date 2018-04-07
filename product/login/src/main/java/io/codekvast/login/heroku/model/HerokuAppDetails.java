package io.codekvast.login.heroku.model;

import lombok.Builder;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class HerokuAppDetails {
    private final String appName;
    private final String contactEmail;
}
