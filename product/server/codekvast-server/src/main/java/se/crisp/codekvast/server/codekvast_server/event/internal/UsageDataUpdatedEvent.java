package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Getter;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Getter
public class UsageDataUpdatedEvent extends CodekvastEvent {
    private final String customerName;
    private final Collection<UsageDataEntry> usageDataEntries;

    public UsageDataUpdatedEvent(Object source, String customerName, Collection<UsageDataEntry> usageDataEntries) {
        super(source);
        this.customerName = customerName;
        this.usageDataEntries = usageDataEntries;
    }

    @Override
    public String toString() {
        return "UsageDataUpdatedEvent(customerName='" + customerName + '\'' + ", usageDataEntries.size=" + usageDataEntries.size() + ")";
    }

    public String toLongString() {
        return "UsageDataUpdatedEvent(customerName='" + customerName + '\'' + ", usageDataEntries=" + usageDataEntries + ")";
    }
}
