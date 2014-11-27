package se.crisp.codekvast.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.constraints.Size;

/**
 * The common header part of all REST messages.
 *
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Header {
    @NonNull
    @Size(min = 1, max = Constraints.MAX_ENVIRONMENT_NAME_LENGTH)
    private String environment;
}
