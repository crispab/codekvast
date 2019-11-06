package io.codekvast.backoffice.event.impl;

import com.google.gson.Gson;
import io.codekvast.backoffice.event.EventHandler;
import io.codekvast.backoffice.rules.FactDAO;
import io.codekvast.backoffice.rules.RuleEngine;
import io.codekvast.common.messaging.model.CodekvastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the event dispatcher.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventHandlerImpl implements EventHandler {

    private final FactDAO factDAO;
    private final RuleEngine ruleEngine;

    @Override
    @Transactional
    public void handle(CodekvastEvent event) {
        factDAO.addFact(event);
        ruleEngine.fireAllRules(event.getCustomerId());
    }
}
