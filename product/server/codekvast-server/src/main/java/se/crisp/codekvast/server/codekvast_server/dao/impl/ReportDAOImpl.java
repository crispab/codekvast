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
import java.util.*;
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
                    MethodUsageScope.POSSIBLY_DEAD, new RetrieveProbablyDeadMethods(),
                    MethodUsageScope.BOOTSTRAP, new RetrieveBootstrapMethods(),
                    MethodUsageScope.LIVE, new RetrieveLiveMethods()
            );

    @Inject
    public ReportDAOImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public int countMethods(long organisationId) {
        log.debug("Counting signatures for organisation {}", organisationId);
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures WHERE organisation_id = ?", Integer.class, organisationId);
    }

    @Override
    public Collection<MethodUsageEntry> getMethodsForScope(MethodUsageScope scope, ReportParameters reportParameters) {
        return methodRetrievalStrategies.get(scope).getMethods(reportParameters);
    }

    @Override
    @Cacheable("report")
    public Collection<Long> getApplicationIds(long organisationId, Collection<String> applicationNames) {
        String placeholders = applicationNames.stream().map(s -> "?").collect(Collectors.joining(","));

        List<Object> args = new ArrayList<>();
        args.add(organisationId);
        args.addAll(applicationNames);

        return jdbcTemplate.queryForList("SELECT id FROM applications WHERE organisation_id = ? AND name IN (" + placeholders + ") " +
                                                 "ORDER BY id ",
                                         Long.class, args.toArray());
    }

    @Override
    @Cacheable("report")
    public Collection<Long> getJvmIdsByAppVersions(long organisationId, Collection<String> applicationVersions) {
        String placeholders = applicationVersions.stream().map(s -> "?").collect(Collectors.joining(","));

        List<Object> args = new ArrayList<>();
        args.add(organisationId);
        args.addAll(applicationVersions);

        return jdbcTemplate
                .queryForList("SELECT id FROM jvm_info WHERE organisation_id = ? AND application_version IN (" + placeholders + ") " +
                                      "ORDER BY id", Long.class, args.toArray());
    }

    private abstract class MethodRetriever {
        abstract Collection<MethodUsageEntry> getMethods(ReportParameters params);

        protected List<Object> generateParams(ReportParameters params) {
            List<Object> result = new ArrayList<>();
            result.add(params.getOrganisationId());
            result.addAll(params.getApplicationIds());
            result.addAll(params.getJvmIds());
            return result;
        }

        protected String generateSql(ReportParameters params) {
            String appIds = params.getApplicationIds().stream().map(s -> "?").collect(Collectors.joining(","));
            String jvmIds = params.getJvmIds().stream().map(s -> "?").collect(Collectors.joining(","));

            return "SELECT DISTINCT s.signature, s.invoked_at_millis FROM signatures s, application_statistics stats, jvm_info jvm " +
                    "WHERE s.organisation_id = ? " +
                    "AND s.application_id IN (" + appIds + ") " +
                    "AND s.jvm_id IN (" + jvmIds + ") " +
                    "AND s.jvm_id = jvm.id " +
                    "AND stats.application_id = s.application_id " +
                    "AND stats.application_version = jvm.application_version ";
        }
    }

    private class RetrieveDeadMethods extends MethodRetriever {

        @Override
        public Collection<MethodUsageEntry> getMethods(ReportParameters params) {
            return jdbcTemplate.query(generateSql(params) + " AND invoked_at_millis = 0 ",
                                      new MethodUsageEntryRowMapper(MethodUsageScope.DEAD),
                                      generateParams(params).toArray());
        }

    }

    private class RetrieveProbablyDeadMethods extends MethodRetriever {
        @Override
        public Collection<MethodUsageEntry> getMethods(ReportParameters params) {
            // TODO: implement
            return Collections.emptyList();
        }
    }

    private class RetrieveBootstrapMethods extends MethodRetriever {
        @Override
        public Collection<MethodUsageEntry> getMethods(ReportParameters params) {
            // TODO: implement
            return Collections.emptyList();
        }
    }

    private class RetrieveLiveMethods extends MethodRetriever {
        @Override
        public Collection<MethodUsageEntry> getMethods(ReportParameters params) {
            // TODO: implement
            return Collections.emptyList();
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
