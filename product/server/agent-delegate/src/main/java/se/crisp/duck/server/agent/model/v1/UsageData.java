package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.Valid;
import java.util.Map;

/**
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = "usage")
public class UsageData {
    @NonNull
    @Valid
    private Header header;

    @NonNull
    private Map<String, Long> usage;
}
