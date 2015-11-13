package se.crisp.codekvast.agent.lib.model;

import lombok.*;

/**
 * Immutable representation of a method signature.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(of = "aspectjString")
@EqualsAndHashCode(of = "aspectjString")
public class MethodSignature {
    @NonNull
    private final String aspectjString;
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
