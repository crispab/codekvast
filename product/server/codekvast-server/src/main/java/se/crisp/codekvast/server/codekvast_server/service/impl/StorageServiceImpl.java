package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.server.agent.model.v1.InvocationData;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent.model.v1.JvmData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.dao.InvocationsDAO;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The implementation of the StorageService.
 *
 * @author Olle Hallin
 */
@Service
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final ApplicationContext applicationContext;
    private final InvocationsDAO invocationsDAO;

    @Inject
    public StorageServiceImpl(ApplicationContext applicationContext, InvocationsDAO invocationsDAO) {
        this.applicationContext = applicationContext;
        this.invocationsDAO = invocationsDAO;
    }

    @Override
    public void storeJvmRunData(JvmData data) throws CodekvastException {
        log.debug("Storing {}", data);

        invocationsDAO.storeJvmData(data);
    }

    @Override
    public void storeSignatureData(SignatureData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        Collection<InvocationEntry> updatedEntries = invocationsDAO.storeInvocationsData(toInitialInvocationsData(data));
        applicationContext.publishEvent(new InvocationDataUpdatedEvent(getClass(), data.getHeader().getCustomerName(), updatedEntries));
    }

    private InvocationData toInitialInvocationsData(SignatureData signatureData) {
        Collection<InvocationEntry> invocationEntries = new ArrayList<>();
        for (String sig : signatureData.getSignatures()) {
            invocationEntries.add(new InvocationEntry(sig, null, null));
        }
        return InvocationData.builder().header(signatureData.getHeader()).jvmFingerprint(signatureData.getJvmFingerprint()).invocations(
                invocationEntries).build();
    }

    @Override
    public void storeInvocationsData(InvocationData data) throws CodekvastException {
        if (log.isTraceEnabled()) {
            log.trace("Storing {}", data.toLongString());
        } else {
            log.debug("Storing {}", data);
        }

        Collection<InvocationEntry> updatedEntries = invocationsDAO.storeInvocationsData(data);
        applicationContext.publishEvent(new InvocationDataUpdatedEvent(getClass(), data.getHeader().getCustomerName(), updatedEntries));
    }

    @Override
    public Collection<InvocationEntry> getSignatures(String customerName) throws CodekvastException {
        return invocationsDAO.getSignatures(customerName);
    }
}
