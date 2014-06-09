package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString(exclude = "signatures")
public class SignatureData {
    @NonNull
    private String customerName;
    @NonNull
    private String appName;
    @NonNull
    private String codeBaseName;
    @NonNull
    private String environment;
    @NonNull
    private Collection<String> signatures;
}
