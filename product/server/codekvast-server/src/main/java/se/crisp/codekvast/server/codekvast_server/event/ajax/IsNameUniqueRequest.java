package se.crisp.codekvast.server.codekvast_server.event.ajax;

import lombok.*;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Send by the registration wizard JavaScript to check whether a name is unique or not.
 *
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class IsNameUniqueRequest {
    @NotBlank
    private String kind;

    @NotBlank
    private String name;
}
