package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
@Data
public class GetCodeUsageRequest {
    @NotNull
    private Collection<String> applications;

    @NotNull
    private Collection<String> versions;

    @NotNull
    private Collection<MethodUsageScope> methods;

    @Min(1)
    private int bootstrapSeconds;

    /**
     * @author olle.hallin@crisp.se
     */
    public enum MethodUsageScope {
        DEAD, PROBABLY_DEAD, BOOTSTRAP, LIVE;
    }
}
