package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class SignatureDataMessage {
    private final Collection<String> usernames;
    private final AppId appId;
    CollectorStatusMessage collectorStatus;
    Collection<SignatureDisplay> signatures;

    @Override
    public String toString() {
        return String.format("SignatureDataMessage[appId=%s, signatures.size()=%d]", appId, signatures.size());
    }
}
