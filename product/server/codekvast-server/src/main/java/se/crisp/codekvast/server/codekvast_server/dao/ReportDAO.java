package se.crisp.codekvast.server.codekvast_server.dao;

import lombok.Builder;
import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageEntry;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageScope;

import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
public interface ReportDAO {
    int countMethods(long organisationId);

    Collection<Long> getJvmIdsByAppVersions(long organisationId, Collection<String> applicationVersions);

    Collection<MethodUsageEntry> getMethodsForScope(MethodUsageScope scope, ReportParameters reportParameters);

    @Value
    @Builder
    class ReportParameters {
        private final long organisationId;
        private final Collection<Long> applicationIds;
        private final Collection<Long> jvmIds;
        private final int bootstrapSeconds;
        private final int usageCycleSeconds;
    }
}
