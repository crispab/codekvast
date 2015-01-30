package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Value;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Value
public class InvocationDataUpdatedEvent {
    private final AppId appId;
    private final Collection<InvocationEntry> invocationEntries;
    private final Collection<String> usernames;

    @Override
    public String toString() {
        return "InvocationDataUpdatedEvent(appId='" + appId + '\'' + ", invocationEntries.size=" + invocationEntries
                .size() + ")";
    }

    public String toLongString() {
        return "InvocationDataUpdatedEvent(appId='" + appId + '\'' + ", invocationEntries=" + invocationEntries + ")";
    }
}
