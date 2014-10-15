package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.event.internal.UsageDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static se.crisp.codekvast.server.codekvast_server.utils.DateTimeUtils.formatDate;

/**
 * The implementation of the StorageService.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class StorageServiceImpl implements StorageService, ApplicationContextAware {

    private final Map<String, UsageDataEntry> usageData = new HashMap<>();

    private ApplicationContext applicationContext;

    @Override
    public void storeSensorData(JvmRunData data) {
        log.debug("Storing {}", data);

        // TODO: store in database
    }

    @Override
    public void storeSignatureData(SignatureData data) {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        synchronized (usageData) {
            for (String sig : data.getSignatures()) {
                storeUsageDataEntry(new UsageDataEntry(sig, 0L, UsageDataEntry.CONFIDENCE_EXACT_MATCH)); // never used
            }
        }
    }

    @Override
    public void storeUsageData(UsageData data) {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        synchronized (usageData) {
            for (UsageDataEntry entry : data.getUsage()) {
                storeUsageDataEntry(entry);
            }
        }
    }

    @Override
    public Collection<UsageDataEntry> getSignatures() {
        synchronized (usageData) {
            return usageData.values();
        }
    }

    private void storeUsageDataEntry(UsageDataEntry entry) {
        String signature = entry.getSignature();
        UsageDataEntry oldEntry = usageData.get(signature);
        if (oldEntry == null) {
            log.debug("Storing signature {}", signature);
            doStoreUsageDataEntry(entry);
        } else if (oldEntry.getUsedAtMillis() < entry.getUsedAtMillis()) {
            log.debug("Signature {} was used at {}", signature, formatDate(entry.getUsedAtMillis()));
            doStoreUsageDataEntry(entry);
        }
    }

    private void doStoreUsageDataEntry(UsageDataEntry entry) {
        usageData.put(entry.getSignature(), entry);
        applicationContext.publishEvent(new UsageDataUpdatedEvent(getClass(), entry));

        // TODO: store in database
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
