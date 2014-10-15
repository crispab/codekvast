package se.crisp.codekvast.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.UUID;

/**
 * REST data about one instrumented JVM.
 * <p/>
 * Should be uploaded regularly during the lifetime of a JVM.
 *
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JvmRunData {
    @NonNull
    @Valid
    private Header header;

    @NonNull
    @Size(min = 1, max = Constraints.MAX_HOST_NAME_LENGTH)
    private String hostName;

    @NonNull
    private UUID uuid;

    private long startedAtMillis;
    private long dumpedAtMillis;
}
