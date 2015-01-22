package se.crisp.codekvast.server.agent_api.model.v1;

import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;

/**
 * Data about one used method.
 *
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
public class InvocationEntry {
    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_SIGNATURE_LENGTH)
    private String signature;

    private Long invokedAtMillis;

    private SignatureConfidence confidence;
}
