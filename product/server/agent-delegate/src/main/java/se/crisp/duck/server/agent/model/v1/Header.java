package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.constraints.Size;

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
    @Size(max = 2)
    private String customerName;

    @NonNull
    @Size(max = Constraints.MAX_APP_NAME_LENGTH)
    private String appName;

    @NonNull
    @Size(max = Constraints.MAX_CODE_BASE_NAME_LENGTH)
    private String codeBaseName;

    @NonNull
    @Size(max = Constraints.MAX_ENVIRONMENT_NAME_LENGTH)
    private String environment;
}
