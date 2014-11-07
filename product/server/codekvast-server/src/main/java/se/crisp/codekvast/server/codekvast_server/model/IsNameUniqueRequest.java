package se.crisp.codekvast.server.codekvast_server.model;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.constraints.NotNull;

/**
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class IsNameUniqueRequest {
    @NotNull
    private String kind;

    @NotNull
    private String name;
}
