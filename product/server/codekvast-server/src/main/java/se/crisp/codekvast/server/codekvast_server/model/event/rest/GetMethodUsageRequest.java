package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * Posted from web layer to get code usage data.
 * @author olle.hallin@crisp.se
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Max(100)
    private int bootstrapSeconds;

}
