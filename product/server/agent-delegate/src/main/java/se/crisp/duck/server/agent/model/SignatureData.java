package se.crisp.duck.server.agent.model;

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
public class SignatureData {
    private String customerName;
    private String appName;
    private String environment;
    private Collection<String> signatures;
}
