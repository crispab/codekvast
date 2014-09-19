package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.agent.model.v1.SensorRunData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.event.UsageDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;
import se.crisp.codekvast.server.codekvast_server.utils.Preconditions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The implementation of the AgentService.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class AgentServiceImpl implements AgentService, ApplicationContextAware {

    private final Map<String, UsageDataEntry> usageData = new HashMap<>();

    private ApplicationContext applicationContext;

    private final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss,SSS");
        }
    };

    @Override
    public void storeSensorData(SensorRunData data) {
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
                storeUsageData(sig, new UsageDataEntry(sig, 0L, UsageDataEntry.CONFIDENCE_EXACT_MATCH)); // never used
            }
        }

        // TODO: store in database
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
                storeUsageData(entry.getSignature(), entry);
            }
        }

        // TODO: store in database
    }

    private void storeUsageData(String signature, UsageDataEntry entry) {
        Preconditions.checkArgument(signature.equals(entry.getSignature()), "signatures does not match");

        UsageDataEntry oldEntry = usageData.get(signature);
        if (oldEntry == null) {
            log.debug("Storing signature {}", signature);
            storeUsageDataEntry(entry);
        } else if (oldEntry.getUsedAtMillis() < entry.getUsedAtMillis()) {
            log.debug("Signature {} was used at {}", signature, formatDate(entry.getUsedAtMillis()));
            storeUsageDataEntry(entry);
        }
    }

    private void storeUsageDataEntry(UsageDataEntry entry) {
        usageData.put(entry.getSignature(), entry);
        applicationContext.publishEvent(new UsageDataUpdatedEvent(getClass(), entry));
    }

    private String formatDate(long timeMillis) {
        return dateFormat.get().format(new Date(timeMillis));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
