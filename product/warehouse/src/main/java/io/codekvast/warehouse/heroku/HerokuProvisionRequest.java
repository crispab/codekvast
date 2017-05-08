package io.codekvast.warehouse.heroku;

import lombok.*;

import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Builder(toBuilder = true)
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class HerokuProvisionRequest {
    private final String heroku_id;
    private final String region;
    private final String plan;
    private final Map<String, String> options;
}
