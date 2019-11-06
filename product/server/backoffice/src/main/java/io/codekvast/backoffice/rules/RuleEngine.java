package io.codekvast.backoffice.rules;

/**
 * A wrapper for Drools.
 *
 * @author olle.hallin@crisp.se
 */
public interface RuleEngine {

    /**
     * Loads all facts for a customer into a Drools stateless session and fires all rule.
     *
     * @param customerId The customer to fire rules for.
     */
    void fireAllRules(Long customerId);
}
