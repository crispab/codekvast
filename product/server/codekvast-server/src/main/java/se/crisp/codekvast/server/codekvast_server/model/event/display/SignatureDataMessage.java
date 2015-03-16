package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import java.util.Collection;

/**
 * A display object posted every time signature data is received from an agent.
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class SignatureDataMessage {
    /**
     * Optional: Which users should be broadcast this message (if logged in)?
     */
    Collection<String> usernames;

    /**
     * Optional: To which app do the signatures belong?
     */
    AppId appId;

    /**
     * Optional collectorStatus.
     * <p/>
     * It is non-null when this object is created by a database query when a user logs in. When this object is broadcast as a result of an
     * agent delivering data, collectorStatus is null.
     */
    CollectorStatusMessage collectorStatus;

    /**
     * The actual signature data.
     */
    @NonNull
    Collection<SignatureDisplay> signatures;

    @Override
    public String toString() {
        return String.format("SignatureDataMessage[appId=%s, signatures.size()=%d]", appId, signatures.size());
    }
}
