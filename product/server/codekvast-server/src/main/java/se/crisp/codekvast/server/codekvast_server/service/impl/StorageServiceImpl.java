package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.dao.StorageDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.UsageDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The implementation of the StorageService.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class StorageServiceImpl implements StorageService {

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
    public void storeSignatureData(SignatureData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        Collection<UsageDataEntry> updatedEntries = storageDAO.storeUsageData(toInitialUsageData(data));
        applicationContext.publishEvent(new UsageDataUpdatedEvent(getClass(), data.getHeader().getCustomerName(), updatedEntries));
    }

    private UsageData toInitialUsageData(SignatureData signatureData) {
        Collection<UsageDataEntry> usageDataEntries = new ArrayList<>();
        for (String sig : signatureData.getSignatures()) {
            usageDataEntries.add(new UsageDataEntry(sig, null, null));
        }
        return UsageData.builder().header(signatureData.getHeader()).jvmFingerprint(signatureData.getJvmFingerprint()).usage(
                usageDataEntries).build();
    }

    @Override
    public void storeUsageData(UsageData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        Collection<UsageDataEntry> updatedEntries = storageDAO.storeUsageData(data);
        applicationContext.publishEvent(new UsageDataUpdatedEvent(getClass(), data.getHeader().getCustomerName(), updatedEntries));
    }

    @Override
    public Collection<UsageDataEntry> getSignatures(String customerName) throws CodekvastException {
        return storageDAO.getSignatures(customerName);
    }
}
