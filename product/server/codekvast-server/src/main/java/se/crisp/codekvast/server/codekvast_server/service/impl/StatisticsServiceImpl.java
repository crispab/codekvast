package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.service.StatisticsService;

import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.google.common.base.Throwables.getRootCause;

/**
 * Asynchronous service for calculating statistics.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final AgentDAO agentDAO;
    private final EventBus eventBus;

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Inject
    public StatisticsServiceImpl(AgentDAO agentDAO, EventBus eventBus) {
        this.agentDAO = agentDAO;
        this.eventBus = eventBus;
    }

    @Override
    public void recalculateApplicationStatistics(AppId appId) {
        executor.execute(new Worker(appId));
    }

    @RequiredArgsConstructor
    private class Worker implements Runnable {

        private final AppId appId;

        @Override
        public void run() {
            try {
                log.debug("Recalculating statistics for {}", appId);
                agentDAO.recalculateApplicationStatistics(appId);
                eventBus.post(agentDAO.createWebSocketMessage(appId.getOrganisationId()));
            } catch (Exception e) {
                log.warn("Cannot calculate statistics for {}: {}", appId, getRootCause(e).toString());
            }

        }
    }
}
