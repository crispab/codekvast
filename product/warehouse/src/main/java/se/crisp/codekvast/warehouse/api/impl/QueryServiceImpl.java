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
import java.util.ArrayList;
import java.util.List;

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
                                   "ORDER BY m.id, j.startedAt, j.dumpedAt, i.invokedAtMillis", rowCallbackHandler, sig);

        return rowCallbackHandler.getResult();
    }

    private class MethodDescriptorRowCallbackHandler implements RowCallbackHandler {
        private final List<MethodDescriptor> result = new ArrayList<>();

        QueryState queryState = new QueryState(-1L);

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            long id = rs.getLong("methodId");
            if (!queryState.isSameMethod(id)) {
                queryState.addTo(result);
                queryState = new QueryState(id);
            }

            long startedAt = rs.getTimestamp("startedAt").getTime();
            long dumpedAt = rs.getTimestamp("dumpedAt").getTime();
            long invokedAtMillis = rs.getLong("invokedAtMillis");

            MethodDescriptor.MethodDescriptorBuilder builder = queryState.getBuilder();
            builder.occursInApplication(ApplicationId.of(rs.getString("appName"), rs.getString("appVersion")),
                                        ApplicationDescriptor
                                                .builder()
                                                .startedAtMillis(startedAt)
                                                .dumpedAtMillis(dumpedAt)
                                                .invokedAtMillis(invokedAtMillis)
                                                .status(SignatureStatus.valueOf(rs.getString("status")))
                                                .build())
                   .collectedInEnvironment(rs.getString("environment"),
                                           EnvironmentDescriptor.builder()
                                                                .hostName(rs.getString("collectorHostname"))
                                                                .tag(rs.getString("tags"))
                                                                .collectedSinceMillis(startedAt)
                                                                .collectedToMillis(dumpedAt)
                                                                .invokedAtMillis(invokedAtMillis)
                                                                .build())
                   .declaringType(rs.getString("declaringType"))
                   .modifiers(rs.getString("modifiers"))
                   .packageName(rs.getString("packageName"))
                   .signature(rs.getString("signature"))
                   .visibility(rs.getString("visibility"));
        }

        private List<MethodDescriptor> getResult() {
            queryState.addTo(result);
            return result;
        }
    }

    @RequiredArgsConstructor
    private class QueryState {
        private final long methodId;

        MethodDescriptor.MethodDescriptorBuilder builder;

        MethodDescriptor.MethodDescriptorBuilder getBuilder() {
            if (builder == null) {
                builder = MethodDescriptor.builder();
            }
            return builder;
        }

        boolean isSameMethod(long id) {
            return id == this.methodId;
        }

        void addTo(List<MethodDescriptor> result) {
            if (builder != null) {
                result.add(builder.build());
            }
        }

    }
}
