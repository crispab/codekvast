package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Getter;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Getter
public class InvocationDataUpdatedEvent extends CodekvastEvent {
    private final AppId appId;
    private final Collection<InvocationEntry> invocationEntries;

    public InvocationDataUpdatedEvent(Object source, AppId appId, Collection<InvocationEntry> invocationEntries) {
        super(source);
        this.appId = appId;
        this.invocationEntries = invocationEntries;
    }

    @Override
    public String toString() {
        return "InvocationDataUpdatedEvent(appId='" + appId + '\'' + ", invocationEntries.size=" + invocationEntries
                .size() + ")";
    }

    public String toLongString() {
        return "InvocationDataUpdatedEvent(appId='" + appId + '\'' + ", invocationEntries=" + invocationEntries + ")";
    }
}
