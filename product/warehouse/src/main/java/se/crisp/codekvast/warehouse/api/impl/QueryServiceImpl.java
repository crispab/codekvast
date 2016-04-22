package se.crisp.codekvast.warehouse.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.warehouse.api.QueryService;
import se.crisp.codekvast.warehouse.api.model.ApplicationDescriptor;
import se.crisp.codekvast.warehouse.api.model.ApplicationId;
import se.crisp.codekvast.warehouse.api.model.EnvironmentDescriptor;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public QueryServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MethodDescriptor> findMethodsBySignature(String signature) {
        String sig = signature == null ? "%" : "%" + signature + "%";
        MethodDescriptorRowCallbackHandler rowCallbackHandler = new MethodDescriptorRowCallbackHandler();

        jdbcTemplate.query("SELECT i.methodId, a.name AS appName, a.version AS appVersion,\n" +
                                   "  i.invokedAtMillis, i.status, j.startedAt, j.dumpedAt, j.environment, j.collectorHostname, j.tags,\n" +
                                   "  m.visibility, m.signature, m.declaringType, m.methodName, m.modifiers, m.packageName\n" +
                                   "  FROM invocations i\n" +
                                   "  JOIN applications a ON a.id = i.applicationId \n" +
                                   "  JOIN methods m ON m.id = i.methodId\n" +
                                   "  JOIN jvms j ON j.id = i.jvmId\n" +
                                   "WHERE m.signature LIKE ?\n" +
                                   "ORDER BY i.methodId ASC, j.startedAt DESC, j.dumpedAt ASC, i.invokedAtMillis ASC", rowCallbackHandler,
                           sig);

        return rowCallbackHandler.getResult();
    }

    private class MethodDescriptorRowCallbackHandler implements RowCallbackHandler {
        private final List<MethodDescriptor> result = new ArrayList<>();

        QueryState queryState = new QueryState(-1L);

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            long id = rs.getLong("methodId");
            String signature = rs.getString("signature");

            if (!queryState.isSameMethod(id)) {
                // The query is sorted on methodId
                log.trace("Found method {}:{}", id, signature);
                queryState.addTo(result);
                queryState = new QueryState(id);
            }

            queryState.countRow();
            long startedAt = rs.getTimestamp("startedAt").getTime();
            long dumpedAt = rs.getTimestamp("dumpedAt").getTime();
            long invokedAtMillis = rs.getLong("invokedAtMillis");

            MethodDescriptor.MethodDescriptorBuilder builder = queryState.getBuilder();
            String appName = rs.getString("appName");
            String appVersion = rs.getString("appVersion");

            queryState.saveApplication(ApplicationDescriptor
                                               .builder()
                                               .name(appName)
                                               .version(appVersion)
                                               .startedAtMillis(startedAt)
                                               .dumpedAtMillis(dumpedAt)
                                               .invokedAtMillis(invokedAtMillis)
                                               .status(SignatureStatus.valueOf(rs.getString("status")))
                                               .build());

            queryState.saveEnvironment(EnvironmentDescriptor.builder()
                                                            .name(rs.getString("environment"))
                                                            .hostName(rs.getString("collectorHostname"))
                                                            .tags(splitOnCommaOrSemicolon(rs.getString("tags")))
                                                            .collectedSinceMillis(startedAt)
                                                            .collectedToMillis(dumpedAt)
                                                            .invokedAtMillis(invokedAtMillis)
                                                            .build());

            builder.declaringType(rs.getString("declaringType"))
                   .modifiers(rs.getString("modifiers"))
                   .packageName(rs.getString("packageName"))
                   .signature(signature)
                   .visibility(rs.getString("visibility"));
        }

        private Set<String> splitOnCommaOrSemicolon(String tags) {
            return new HashSet<>(Arrays.asList(tags.split("\\s*[,;]\\s")));
        }

        private List<MethodDescriptor> getResult() {
            queryState.addTo(result);
            return result;
        }
    }

    @RequiredArgsConstructor
    private class QueryState {
        private final long methodId;

        private final Map<ApplicationId, ApplicationDescriptor> applications = new HashMap<>();
        private final Map<String, EnvironmentDescriptor> environments = new HashMap<>();

        MethodDescriptor.MethodDescriptorBuilder builder;
        private int rows;

        MethodDescriptor.MethodDescriptorBuilder getBuilder() {
            if (builder == null) {
                builder = MethodDescriptor.builder().id(methodId);
            }
            return builder;
        }

        boolean isSameMethod(long id) {
            return id == this.methodId;
        }

        void saveApplication(ApplicationDescriptor applicationDescriptor) {
            ApplicationId appId = ApplicationId.of(applicationDescriptor);
            applications.put(appId, applicationDescriptor.mergeWith(applications.get(appId)));

        }

        void saveEnvironment(EnvironmentDescriptor environmentDescriptor) {
            String name = environmentDescriptor.getName();
            environments.put(name, environmentDescriptor.mergeWith(environments.get(name)));
        }

        void addTo(List<MethodDescriptor> result) {
            if (builder != null) {
                log.debug("Adding method {} to result ({} result set rows)", methodId, rows);
                builder.occursInApplications(new TreeSet<>(applications.values()));
                builder.collectedInEnvironments(new TreeSet<>(environments.values()));
                result.add(builder.build());
            }
        }

        void countRow() {
            rows += 1;
        }

    }
}
