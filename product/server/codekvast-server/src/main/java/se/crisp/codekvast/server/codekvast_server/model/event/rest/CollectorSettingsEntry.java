package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Data;
import se.crisp.codekvast.server.agent_api.model.v1.Constraints;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author olle.hallin@crisp.se
 */
@Data
public class CollectorSettingsEntry {
    @NotNull
    @Size(max = Constraints.MAX_APP_NAME_LENGTH)
    private String name;

    @Min(1)
    int trulyDeadAfterDays;
}
