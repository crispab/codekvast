package se.crisp.codekvast.server.daemon_api.model.v1;

import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * Data about one used method.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "signature")
@Builder
public class SignatureEntry {
    @NonNull
    @NotBlank
    @Size(max = Constraints.MAX_SIGNATURE_LENGTH)
    private String signature;

    @NonNull
    @Min(0)
    private Long invokedAtMillis;

    @NonNull
    @Min(0)
    @Max(10 * 365 * 24 * 60 * 60 * 1000L) // ten years uptime is mighty unusal!
    private Long millisSinceJvmStart;

    @AssertTrue
    public boolean assertValid() {
        return ((invokedAtMillis == 0L) && (millisSinceJvmStart == 0L)) || ((invokedAtMillis > 0L) && (millisSinceJvmStart > 0L));
    }

    @NonNull
    private SignatureConfidence confidence;
}
