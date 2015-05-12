package se.crisp.codekvast.server.codekvast_server.dao.impl;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.codekvast_server.dao.ReportDAO;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageEntry;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.MethodUsageScope;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DAO responsible for retrieving report data.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
@Repository
public class ReportDAOImpl extends AbstractDAOImpl implements ReportDAO {

    private final Map<MethodUsageScope, MethodRetriever> methodRetrievalStrategies =
            (Map<MethodUsageScope, MethodRetriever>) ImmutableMap.of(
                    MethodUsageScope.DEAD, new RetrieveDeadMethods(),
                    MethodUsageScope.POSSIBLY_DEAD, new RetrievePossiblyDeadMethods(),
                    MethodUsageScope.BOOTSTRAP, new RetrieveBootstrapMethods(),
                    MethodUsageScope.LIVE, new RetrieveLiveMethods()
            );

    @Inject
    public ReportDAOImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public int countMethods(long organisationId) {
        long startedAt = System.currentTimeMillis();
        Integer result =
                jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures WHERE organisation_id = ?", Integer.class, organisationId);
        log.debug("Counted {} signatures for organisation {} in {} ms", result, organisationId, System.currentTimeMillis() - startedAt);
        return result;
    }

    @Override
    public Collection<MethodUsageEntry> getMethodsForScope(MethodUsageScope scope, ReportParameters reportParameters) {
        return methodRetrievalStrategies.get(scope).getMethods(scope, reportParameters);
    }

    @Override
    @Cacheable("report")
    public Collection<Long> getJvmIdsByAppVersions(long organisationId, Collection<String> applicationVersions) {
        String condition = applicationVersions.size() == 1 ? "= ?"
                : "IN (" + applicationVersions.stream().map(s -> "?").collect(Collectors.joining(",")) + ")";

        List<Object> args = new ArrayList<>();
        args.add(organisationId);
        args.addAll(applicationVersions);

        return jdbcTemplate.queryForList(
                "SELECT id FROM jvm_info WHERE organisation_id = ? AND application_version " + condition + "ORDER BY id",
                Long.class, args.toArray());
    }

    private abstract class MethodRetriever {
        abstract Collection<MethodUsageEntry> doGetMethods(ReportParameters reportParameters);

        Collection<MethodUsageEntry> getMethods(MethodUsageScope scope, ReportParameters reportParameters) {
            long startedAt = System.currentTimeMillis();

            Collection<MethodUsageEntry> result = doGetMethods(reportParameters);

            log.debug("Retrieved {} {} methods in {} ms", result.size(), scope.toDisplayString(), System.currentTimeMillis() - startedAt);
            return result;
        }

        protected List<Object> generateParams(ReportParameters reportParameters) {
            List<Object> params = new ArrayList<>();
            params.add(reportParameters.getOrganisationId());
            params.addAll(reportParameters.getApplicationIds());
            params.addAll(reportParameters.getJvmIds());
            return params;
        }

        protected String generateSql(ReportParameters reportParameters) {
            String appIds = reportParameters.getApplicationIds().stream().map(s -> "?").collect(Collectors.joining(","));
            String jvmIds = reportParameters.getJvmIds().stream().map(s -> "?").collect(Collectors.joining(","));

            String sql = "SELECT DISTINCT s.signature, s.invoked_at_millis FROM signatures s, application_statistics stats, jvm_info jvm " +
                    "WHERE s.organisation_id = ? " +
                    "AND s.application_id IN (" + appIds + ") " +
                    "AND s.jvm_id IN (" + jvmIds + ") " +
                    "AND s.jvm_id = jvm.id " +
                    "AND stats.application_id = s.application_id " +
                    "AND stats.application_version = jvm.application_version ";
            sql = sql.replaceAll("IN \\(\\?\\)", "= ?");
            return sql;
        }
    }

    private class RetrieveDeadMethods extends MethodRetriever {

        @Override
        public Collection<MethodUsageEntry> doGetMethods(ReportParameters reportParameters) {
            return jdbcTemplate.query(generateSql(reportParameters) + " AND invoked_at_millis = 0 ",
                                      new MethodUsageEntryRowMapper(MethodUsageScope.DEAD),
                                      generateParams(reportParameters).toArray());
        }

    }

    private class RetrievePossiblyDeadMethods extends MethodRetriever {
        @Override
        public Collection<MethodUsageEntry> doGetMethods(ReportParameters reportParameters) {
            List<Object> params = generateParams(reportParameters);
            params.add(reportParameters.getBootstrapSeconds() * 1000L);
            params.add(reportParameters.getUsageCycleSeconds() * 1000L);

            String sql = generateSql(reportParameters) +
                    "AND s.invoked_at_millis > stats.max_started_at_millis + ? " +
                    "AND s.invoked_at_millis <= stats.last_reported_at_millis - ? ";

            return jdbcTemplate.query(sql, new MethodUsageEntryRowMapper(MethodUsageScope.POSSIBLY_DEAD), params.toArray());
        }
    }

    private class RetrieveBootstrapMethods extends MethodRetriever {
        @Override
        public Collection<MethodUsageEntry> doGetMethods(ReportParameters reportParameters) {
            List<Object> params = generateParams(reportParameters);
            params.add(reportParameters.getBootstrapSeconds() * 1000L);

            String sql = generateSql(reportParameters) +
                    "AND s.invoked_at_millis >= stats.max_started_at_millis " +
                    "AND s.millis_since_jvm_start < ? ";

            return jdbcTemplate.query(sql, new MethodUsageEntryRowMapper(MethodUsageScope.BOOTSTRAP), params.toArray());
        }
    }

    private class RetrieveLiveMethods extends MethodRetriever {
        @Override
        public Collection<MethodUsageEntry> doGetMethods(ReportParameters reportParameters) {
            List<Object> params = generateParams(reportParameters);
            params.add(reportParameters.getUsageCycleSeconds() * 1000L);

            String sql = generateSql(reportParameters) +
                    "AND s.invoked_at_millis >= stats.last_reported_at_millis - ? ";

            return jdbcTemplate.query(sql, new MethodUsageEntryRowMapper(MethodUsageScope.LIVE), params.toArray());
        }
    }

    @RequiredArgsConstructor
    private static class MethodUsageEntryRowMapper implements RowMapper<MethodUsageEntry> {
        private final MethodUsageScope scope;

        @Override
        public MethodUsageEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            return MethodUsageEntry.builder()
                                   .name(rs.getString(1))
                                   .scope(scope)
                                   .invokedAtMillis(rs.getLong(2))
                                   .build();

        }
    }
}
