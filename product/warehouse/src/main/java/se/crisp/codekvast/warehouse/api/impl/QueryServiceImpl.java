package se.crisp.codekvast.warehouse.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.warehouse.api.QueryService;
import se.crisp.codekvast.warehouse.api.model.ApplicationDescriptor;
import se.crisp.codekvast.warehouse.api.model.EnvironmentDescriptor;
import se.crisp.codekvast.warehouse.api.model.MethodDescriptor;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
                                   "ORDER BY m.id ASC, i.invokedAtMillis ASC", rowCallbackHandler, sig);

        return rowCallbackHandler.getResult();
    }

    private class MethodDescriptorRowCallbackHandler implements RowCallbackHandler {
        private final List<MethodDescriptor> result = new ArrayList<>();

        State state = new State(-1L);

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            long id = rs.getLong("methodId");
            if (!state.isSameMethod(id)) {
                state.addTo(result);
                state = new State(id);
            }
            Long invokedAtMillis = rs.getLong("invokedAtMillis");
            Timestamp startedAt = rs.getTimestamp("startedAt");
            Timestamp dumpedAt = rs.getTimestamp("dumpedAt");

            MethodDescriptor.MethodDescriptorBuilder builder = state.getBuilder();
            builder.occursInApplication(ApplicationDescriptor
                                                .builder()
                                                .name(rs.getString("appName"))
                                                .version(rs.getString("appVersion"))
                                                .invokedAtMillis(invokedAtMillis)
                                                .status(SignatureStatus.valueOf(rs.getString("status")))
                                                .build())
                   .collectedInEnvironment(EnvironmentDescriptor.builder()
                                                                .name(rs.getString("environment"))
                                                                .hostName(rs.getString("collectorHostname"))
                                                                .tag(rs.getString("tags"))
                                                                .collectedSinceMillis(startedAt.getTime())
                                                                .collectedDays(getDaysBetween(startedAt, dumpedAt))
                                                                .build())
                   .declaringType(rs.getString("declaringType"))
                   .modifiers(rs.getString("modifiers"))
                   .packageName(rs.getString("packageName"))
                   .signature(rs.getString("signature"))
                   .visibility(rs.getString("visibility"));

            state.updateLastInvokedAtMillis(invokedAtMillis)
                 .updateMinStartedAt(startedAt)
                 .updateMaxDumpedAt(dumpedAt);
        }

        public List<MethodDescriptor> getResult() {
            state.addTo(result);
            return result;
        }

        @RequiredArgsConstructor
        private class State {
            private final long methodId;

            Long lastInvokedAtMillis = 0L;
            Timestamp minStartedAt = new Timestamp(Long.MAX_VALUE);
            Timestamp maxDumpedAt = new Timestamp(0L);
            MethodDescriptor.MethodDescriptorBuilder builder;

            MethodDescriptor.MethodDescriptorBuilder getBuilder() {
                if (builder == null) {
                    builder = MethodDescriptor.builder();
                }
                return builder;
            }

            State updateLastInvokedAtMillis(long invokedAtMillis) {
                this.lastInvokedAtMillis = Math.max(this.lastInvokedAtMillis, invokedAtMillis);
                return this;
            }

            State updateMinStartedAt(Timestamp startedAt) {
                if (startedAt.before(minStartedAt)) {
                    minStartedAt = startedAt;
                }
                return this;
            }

            State updateMaxDumpedAt(Timestamp dumpedAt) {
                if (dumpedAt.after(maxDumpedAt)) {
                    maxDumpedAt = dumpedAt;
                }
                return this;
            }

            public boolean isSameMethod(long id) {
                return id == this.methodId;
            }

            public void addTo(List<MethodDescriptor> result) {
                if (builder != null) {
                    result.add(builder.lastInvokedAtMillis(lastInvokedAtMillis)
                                      .collectedSinceMillis(minStartedAt.getTime())
                                      .collectedDays(getDaysBetween(minStartedAt, maxDumpedAt))
                                      .build());
                }
            }

        }

        private int getDaysBetween(Timestamp first, Timestamp last) {
            int millisPerDay = 24 * 60 * 60 * 1000;
            return (int) (last.getTime() - first.getTime()) / millisPerDay;
        }
    }
}
