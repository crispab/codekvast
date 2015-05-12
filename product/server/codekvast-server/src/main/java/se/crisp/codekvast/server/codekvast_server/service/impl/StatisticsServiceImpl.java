package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.service.StatisticsService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.*;

import static com.google.common.base.Throwables.getRootCause;

/**
 * Asynchronous service for calculating statistics. It serves two purposes:
 * <ol>
 *     <li>Avoid unnecessary recalculations for the same app if there already is one in progress</li>
 *     <li>Eliminate the risk for database lock conflicts</li>
 * </ol>
 *
 * setting codekvast.statisticsDelayMillis to <= 0 turns off the async behaviour.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final AgentDAO agentDAO;
    private final EventBus eventBus;
    private final CodekvastSettings codekvastSettings;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final BlockingQueue<StatisticsRequest> queue = new DelayQueue<>();

    @Inject
    public StatisticsServiceImpl(AgentDAO agentDAO, EventBus eventBus, CodekvastSettings codekvastSettings) {
        this.agentDAO = agentDAO;
        this.eventBus = eventBus;
        this.codekvastSettings = codekvastSettings;
    }

    @PostConstruct
    void start() {
        log.debug("Starting");
        executor.execute(new Worker());
    }

    @PreDestroy
    void shutdown() {
        log.debug("Shutting down");
        executor.shutdownNow();
    }

    @Override
    @Synchronized
    public void recalculateApplicationStatistics(AppId appId) {

        long statisticsDelayMillis = codekvastSettings.getStatisticsDelayMillis();

        if (statisticsDelayMillis <= 0L) {
            recalculate(appId, true);
            return;
        }

        if (executor.isShutdown()) {
            log.debug("Ignoring statistics request during shutdown");
            return;
        }

        StatisticsRequest statisticsRequest = new StatisticsRequest(appId, statisticsDelayMillis);

        if (queue.contains(statisticsRequest)) {
            log.debug("{} is already queued for statistics", appId);
        } else {
            log.debug("Queueing statistics for {}", appId);
            queue.add(statisticsRequest);
        }
    }

    @Override
    public void recalculateApplicationStatistics(long organisationId, Collection<String> applicationNames) {
        if (!applicationNames.isEmpty()) {
            log.info("Recalculating application statistics for organisation {} {}...", organisationId, applicationNames);

            long startedAt = System.currentTimeMillis();

            Collection<AppId> appIds = agentDAO.getApplicationIds(organisationId, applicationNames);

            // Eliminate duplicates that only differs on JVM id...
            appIds.stream()
                  .map(id -> AppId.builder()
                                  .organisationId(id.getOrganisationId())
                                  .appId(id.getAppId())
                                  .appVersion(id.getAppVersion())
                                  .jvmId(0)
                                  .build())
                  .distinct()
                  .forEach(appId -> recalculate(appId, false));

            log.info("Calculated statistics for organisation {} in {} ms", organisationId, System.currentTimeMillis() - startedAt);

            eventBus.post(agentDAO.createWebSocketMessage(organisationId));
        }
    }

    private void recalculate(AppId appId, boolean postOnEventBus) {
        log.debug("Recalculating statistics for {}", appId);

        long startedAt = System.currentTimeMillis();
        agentDAO.recalculateApplicationStatistics(appId);
        log.info("Calculated statistics for {} in {} ms", appId, System.currentTimeMillis() - startedAt);

        if (postOnEventBus) {
            eventBus.post(agentDAO.createWebSocketMessage(appId.getOrganisationId()));
        }
    }

    @RequiredArgsConstructor
    private class Worker implements Runnable {

        @Override
        public void run() {
            for (; ; ) {
                AppId appId = null;
                try {
                    appId = queue.take().getAppId();

                    recalculate(appId, true);
                } catch (InterruptedException ignore) {
                    log.debug("Interrupted");
                    return;
                } catch (Exception e) {
                    log.warn("Cannot calculate statistics for {}: {}", appId, getRootCause(e).toString());
                }
            }
        }
    }

    @EqualsAndHashCode(of = "appId")
    private class StatisticsRequest implements Delayed {

        @Getter
        private final AppId appId;
        private final long dueAtMillis;

        private StatisticsRequest(AppId appId, long delayMillis) {
            this.appId = appId;
            dueAtMillis = System.currentTimeMillis() + delayMillis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = dueAtMillis - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (this.dueAtMillis < ((StatisticsRequest) o).dueAtMillis) {
                return -1;
            }
            if (this.dueAtMillis > ((StatisticsRequest) o).dueAtMillis) {
                return 1;
            }
            return 0;
        }
    }
}
