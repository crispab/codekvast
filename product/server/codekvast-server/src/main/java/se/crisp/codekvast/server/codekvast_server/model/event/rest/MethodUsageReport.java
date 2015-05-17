package se.crisp.codekvast.server.codekvast_server.model.event.rest;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;

import java.util.Collection;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class MethodUsageReport {

    @NonNull
    private final GetMethodUsageRequest request;

    @NonNull
    private final String username;

    private final int reportId;

    private long reportExpiresAtMillis;

    private final Map<MethodUsageScope, Integer> numMethodsByScope;
    private final Collection<MethodUsageEntry> methods;

    private final Collection<ReportService.Format> availableFormats;

}
