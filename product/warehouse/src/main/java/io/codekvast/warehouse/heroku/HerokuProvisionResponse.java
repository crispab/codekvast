package io.codekvast.warehouse.heroku;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class HerokuProvisionResponse {

    private final String id;

    private final Map<String, String> config;
}
