package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Getter;
import lombok.ToString;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;

/**
 * @author Olle Hallin
 */
@Getter
@ToString(callSuper = true)
public class UsageDataUpdatedEvent extends CodekvastEvent {
    private final UsageDataEntry usageDataEntry;

    public UsageDataUpdatedEvent(Object source, UsageDataEntry usageDataEntry) {
        super(source);
        this.usageDataEntry = usageDataEntry;
    }
}
