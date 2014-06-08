package se.crisp.duck.server.agent.model.v1;

import lombok.*;
import lombok.experimental.Builder;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString(exclude = "signatures")
public class SignatureData {
    private String customerName;
    private String appName;
    private String environment;
    private Collection<String> signatures;
}
