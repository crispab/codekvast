package se.crisp.codekvast.warehouse.file_import;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class Application {
    @NonNull
    private final Long localId;
    @NonNull
    private final String name;
    @NonNull
    private final String version;
    @NonNull
    private final Long createdAtMillis;
}
