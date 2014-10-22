package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.agent.model.v1.*;
import se.crisp.codekvast.server.codekvast_server.dao.StorageDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.UsageDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static se.crisp.codekvast.server.codekvast_server.utils.DateTimeUtils.formatDate;

/**
 * The implementation of the StorageService.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final Map<String, UsageDataEntry> usageData = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;
    private final StorageDAO storageDAO;

    @Inject
    public StorageServiceImpl(ApplicationContext applicationContext, StorageDAO storageDAO) {
        this.applicationContext = applicationContext;
        this.storageDAO = storageDAO;
    }

    @Override
    public void storeJvmRunData(JvmRunData data) throws CodekvastException {
        log.debug("Storing {}", data);

        storageDAO.storeJvmRunData(data);
    }

    @Override
    public void storeSignatureData(SignatureData signatureData) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", signatureData.toLongString());
        } else {
            log.debug("Storing {}", signatureData);
        }
        storeUsageData(toInitialUsageData(signatureData));
    }

    private UsageData toInitialUsageData(SignatureData signatureData) {
        Collection<UsageDataEntry> usageDataEntries = new ArrayList<>();
        for (String sig : signatureData.getSignatures()) {
            usageDataEntries.add(new UsageDataEntry(sig, 0L, UsageConfidence.EXACT_MATCH)); // never used
        }
        return UsageData.builder().header(signatureData.getHeader()).usage(usageDataEntries).build();
    }

    @Override
    public void storeUsageData(UsageData data) {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        Collection<UsageDataEntry> updatedEntries = new ArrayList<>();
        for (UsageDataEntry entry : data.getUsage()) {
            storeUsageDataEntry(updatedEntries, entry);
        }
        applicationContext.publishEvent(new UsageDataUpdatedEvent(getClass(), updatedEntries));
    }

    @Override
    public Collection<UsageDataEntry> getSignatures() {
        return new ArrayList<>(usageData.values());
    }

    private void storeUsageDataEntry(Collection<UsageDataEntry> updatedEntries, UsageDataEntry entry) {
        String signature = entry.getSignature();
        UsageDataEntry oldEntry = usageData.get(signature);
        if (oldEntry == null) {
            log.debug("Storing signature {}", signature);
            doStoreUsageDataEntry(updatedEntries, entry);
        } else if (oldEntry.getUsedAtMillis() < entry.getUsedAtMillis()) {
            log.debug("Signature {} was used at {}", signature, formatDate(entry.getUsedAtMillis()));
            doStoreUsageDataEntry(updatedEntries, entry);
        }
    }

    private void doStoreUsageDataEntry(Collection<UsageDataEntry> updatedEntries, UsageDataEntry entry) {
        usageData.put(entry.getSignature(), entry);
        updatedEntries.add(entry);
        // TODO: store in database
    }
}
