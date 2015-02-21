package se.crisp.codekvast.server.codekvast_server.model.event.internal;

import lombok.Value;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import java.util.Collection;

/**
 * An EventBus event broadcast when invocation data has been received from an agent.
 * @author olle.hallin@crisp.se
 */
@Value
public class InvocationDataReceivedEvent {
    /**
     * The application from which data has been received.
     */
    AppId appId;

    /**
     * The raw signature data received from the agent.
     */
    Collection<SignatureEntry> invocationEntries;

    @Override
    public String toString() {
        return "InvocationDataReceivedEvent(appId=" + appId + ", invocationEntries.size=" + invocationEntries
                .size() + ")";
    }
}
