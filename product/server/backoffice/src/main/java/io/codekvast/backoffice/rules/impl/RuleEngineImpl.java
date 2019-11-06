package io.codekvast.backoffice.rules.impl;

import io.codekvast.backoffice.rules.FactDAO;
import io.codekvast.backoffice.rules.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEngineImpl implements RuleEngine {

    private final FactDAO factDAO;

    @Override
    public void fireAllRules(Long customerId) {
        List<Object> facts = factDAO.getFacts(customerId);
        logger.debug("Loading {} into rule session", facts);
    }
}
