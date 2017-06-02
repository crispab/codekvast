package io.codekvast.warehouse.retention;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically performs data retention, i.e., removes data according to customers' price plans.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class RetentionTask {

    /**
     * Scheduled task that invokes the data retention service.
     */
    @Scheduled(
        initialDelayString = "${codekvast.dataRetentionInitialDelaySeconds}000",
        fixedDelayString = "${codekvast.dataRetentionIntervalSeconds}000")
    public void performDataRetention() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("Codekvast Data Retention");
        try {
            log.info("Performing data retention");

        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

}
