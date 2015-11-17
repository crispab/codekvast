package se.crisp.codekvast.warehouse.file_import;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class Method {
    @NonNull
    private final Long localId;
    @NonNull
    private final String visibility;
    @NonNull
    private final String signature;
    @NonNull
    private final Long createdAtMillis;
    @NonNull
    private final String declaringType;
    @NonNull
    private final String exceptionTypes;
    @NonNull
    private final String methodName;
    @NonNull
    private final String modifiers;
    @NonNull
    private final String packageName;
    @NonNull
    private final String parameterTypes;
    @NonNull
    private final String returnType;
}
