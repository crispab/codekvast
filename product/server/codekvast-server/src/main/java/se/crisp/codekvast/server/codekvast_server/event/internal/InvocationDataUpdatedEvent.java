package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Getter;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
@Getter
public class InvocationDataUpdatedEvent extends CodekvastEvent {
    private final String customerName;
    private final Collection<InvocationEntry> invocationEntries;

    public InvocationDataUpdatedEvent(Object source, String customerName, Collection<InvocationEntry> invocationEntries) {
        super(source);
        this.customerName = customerName;
        this.invocationEntries = invocationEntries;
    }

    @Override
    public String toString() {
        return "InvocationDataUpdatedEvent(customerName='" + customerName + '\'' + ", invocationEntries.size=" + invocationEntries
                .size() + ")";
    }

    public String toLongString() {
        return "InvocationDataUpdatedEvent(customerName='" + customerName + '\'' + ", invocationEntries=" + invocationEntries + ")";
    }
}
