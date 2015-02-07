package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Value;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
@Value
public class InvocationDataReceivedEvent {
    private final AppId appId;
    private final Collection<SignatureEntry> invocationEntries;

    @Override
    public String toString() {
        return "InvocationDataReceivedEvent(appId=" + appId + ", invocationEntries.size=" + invocationEntries
                .size() + ")";
    }
}
