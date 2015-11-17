package se.crisp.codekvast.warehouse.file_import;

import lombok.Builder;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class Application {
    private final Long localId;
    private final String name;
    private final String version;
    private final Long createdAtMillis;
}
