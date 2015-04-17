package se.crisp.codekvast.agent.main;

import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;

import java.util.List;

/**
 * InvocationsCollector keeps track of which invocations have not yet been uploaded to the codekvast-server. It also makes sure that an
 * older invocation is not uploaded after a younger invocation.
 *
 * @author olle.hallin@crisp.se
 */
public interface InvocationsCollector {
    List<SignatureEntry> getNotUploadedInvocations(String jvmUuid);

    void put(String jvmUuid, long jvmStartedAtMillis, String signature, long invokedAtMillis, SignatureConfidence
            confidence);

    void clearNotUploadedSignatures(String jvmUuid);
}
