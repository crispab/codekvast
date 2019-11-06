package io.codekvast.backoffice.rules;

import io.codekvast.common.messaging.model.CodekvastEvent;

import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
public interface FactDAO {

    void addFact(CodekvastEvent event);

    List<Object> getFacts(Long customerId);
}
