package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Getter;
import lombok.ToString;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Getter
@ToString(callSuper = true)
public class UsageDataUpdatedEvent extends CodekvastEvent {
    private final Collection<UsageDataEntry> usageDataEntries;

    public UsageDataUpdatedEvent(Object source, Collection<UsageDataEntry> usageDataEntries) {
        super(source);
        this.usageDataEntries = usageDataEntries;
    }
}
