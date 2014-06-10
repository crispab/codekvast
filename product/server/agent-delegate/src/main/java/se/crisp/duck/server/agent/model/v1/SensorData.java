package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import java.util.UUID;

/**
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SensorData {
    @NonNull
    private Header header;
    @NonNull
    private String hostName;
    @NonNull
    private UUID uuid;
    private long startedAtMillis;
    private long dumpedAtMillis;
}
