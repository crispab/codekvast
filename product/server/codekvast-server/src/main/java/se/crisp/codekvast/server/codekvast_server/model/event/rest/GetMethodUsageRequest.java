package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * Posted from web layer to get code usage data.
 * @author olle.hallin@crisp.se
 */
@Data
public class GetMethodUsageRequest {
    @NotNull
    private Collection<String> applications;

    @NotNull
    private Collection<String> versions;

    @NotNull
    private Collection<MethodUsageScope> methodUsageScopes;

    @Min(1)
    private Integer maxPreviewRows;

    @Min(1)
    private int usageCycleSeconds;

    @Min(1)
    private int bootstrapSeconds;

}
