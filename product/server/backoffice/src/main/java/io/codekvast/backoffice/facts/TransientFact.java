package io.codekvast.backoffice.facts;

import io.codekvast.backoffice.rules.RuleEngine;
import io.codekvast.common.messaging.model.CodekvastEvent;

/**
 * Marker interface for facts that are *NOT* persisted in the database.
 *
 * They are instead constructed by querying CustomerService in {@link RuleEngine#handle(CodekvastEvent)}*
 *
 * @author olle.hallin@crisp.se
 */
public interface TransientFact {
}
