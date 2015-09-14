package se.crisp.codekvast.agent.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.agent.codebase.CodeBase;
import se.crisp.codekvast.agent.model.Invocation;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;

import javax.inject.Inject;
import java.util.List;

/**
 * Helper class for moving invocation data from several files into the local database inside one big transaction.
 */
@Component
@Slf4j
public class TransactionHelper {

    private final InvocationsCollector invocationsCollector;

    @Inject
    public TransactionHelper(InvocationsCollector invocationsCollector) {
        this.invocationsCollector = invocationsCollector;
    }

    @Transactional
    public void storeNormalizedInvocations(JvmState jvmState, List<Invocation> invocations) {
        CodeBase codeBase = jvmState.getCodeBase();

        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Invocation invocation : invocations) {
            String rawSignature = invocation.getSignature();
            String normalizedSignature = codeBase.normalizeSignature(rawSignature);
            String baseSignature = codeBase.getBaseSignature(normalizedSignature);

            SignatureConfidence confidence = null;
            if (normalizedSignature == null) {
                ignored += 1;
            } else if (codeBase.hasSignature(normalizedSignature)) {
                recognized += 1;
                confidence = SignatureConfidence.EXACT_MATCH;
            } else if (baseSignature != null) {
                overridden += 1;
                confidence = SignatureConfidence.FOUND_IN_PARENT_CLASS;
                log.debug("{} replaced by {}", normalizedSignature, baseSignature);
                normalizedSignature = baseSignature;
            } else if (normalizedSignature.equals(rawSignature)) {
                unrecognized += 1;
                confidence = SignatureConfidence.NOT_FOUND_IN_CODE_BASE;
                log.debug("Unrecognized signature: {}", normalizedSignature);
            } else {
                unrecognized += 1;
                confidence = SignatureConfidence.NOT_FOUND_IN_CODE_BASE;
                log.debug("Unrecognized signature: {} (was {})", normalizedSignature, rawSignature);
            }

            if (normalizedSignature != null) {
                invocationsCollector.put(jvmState.getJvm().getJvmUuid(),
                                         jvmState.getJvm().getStartedAtMillis(),
                                         normalizedSignature,
                                         invocation.getInvokedAtMillis(),
                                         confidence);
            }
        }

        FileUtils.deleteAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());

        // For debugging...
        codeBase.writeSignaturesToDisk();

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored method invocations applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.debug("{} signature invocations applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
    }

}
