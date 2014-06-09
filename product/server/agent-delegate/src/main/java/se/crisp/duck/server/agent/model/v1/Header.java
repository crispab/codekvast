package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

/**
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Header {
    @NonNull
    private String customerName;

    @NonNull
    private String appName;

    @NonNull
    private String codeBaseName;

    @NonNull
    private String environment;
}
