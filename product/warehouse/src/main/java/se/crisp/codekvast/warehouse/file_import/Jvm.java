package se.crisp.codekvast.warehouse.file_import;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class Jvm {
    @NonNull
    private final Long localId;
    @NonNull
    private final String uuid;
    @NonNull
    private final Long startedAtMillis;
    @NonNull
    private final Long dumpedAtMillis;
    @NonNull
    private final String jvmDataJson;
}
