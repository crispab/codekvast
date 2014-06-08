package se.crisp.duck.server.agent.model;

import lombok.Value;
import lombok.experimental.Builder;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Value
@Builder
public class UploadSignatureData {
    private final String customerName;
    private final String appName;
    private final String environment;
    private final Collection<String> signatures;
}
