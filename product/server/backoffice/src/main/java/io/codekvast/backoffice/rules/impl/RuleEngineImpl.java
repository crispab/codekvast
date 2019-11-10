/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.backoffice.rules.impl;

import io.codekvast.backoffice.facts.PersistentFact;
import io.codekvast.backoffice.rules.RuleEngine;
import io.codekvast.backoffice.service.MailSender;
import io.codekvast.common.messaging.model.CodekvastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.event.rule.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEngineImpl implements RuleEngine {

    private final FactDAO factDAO;
    private final MailSender mailSender;
    private final Clock clock;

    private KieServices kieServices = KieServices.Factory.get();
    private KieContainer kieContainer = kieServices.getKieClasspathContainer();

    @Override
    @Transactional
    public void handle(CodekvastEvent event) {
        final Long customerId = event.getCustomerId();

        KieSession session = kieContainer.newKieSession();
        session.setGlobal("clock", clock);
        session.setGlobal("customerId", customerId);
        session.setGlobal("mailSender", mailSender);

        // session.addEventListener(new DebugAgendaEventListener());

        // First load all old facts from the database, and remember their fact handles...
        Map<FactHandle, Long> factHandleMap = new HashMap<>();
        for (FactWrapper w : factDAO.getFacts(customerId)) {
            FactHandle handle = session.insert(w.getFact());
            factHandleMap.put(handle, w.getId());
        }

        // Add this event as a transient fact...
        session.insert(event);

        // Attach an event listener that will persist all changes caused by the event...
        session.addEventListener(new FactPersistenceEventListener(factHandleMap, customerId));

        // Fire the rules...
        session.fireAllRules();
        session.dispose();
    }

    @RequiredArgsConstructor
    private class FactPersistenceEventListener implements RuleRuntimeEventListener {
        private final Map<FactHandle, Long> factHandleMap;
        private final Long customerId;

        @Override
        public void objectInserted(ObjectInsertedEvent event) {
            Object object = event.getObject();
            if (object instanceof PersistentFact) {
                Long id = factDAO.addFact(customerId, (PersistentFact) object);
                logger.debug("Added fact {}:{}:{}", id, customerId, object);
                factHandleMap.put(event.getFactHandle(), id);
            }
        }

        @Override
        public void objectUpdated(ObjectUpdatedEvent event) {
            PersistentFact object = (PersistentFact) event.getObject();
            Long id = factHandleMap.get(event.getFactHandle());
            if (object instanceof PersistentFact && id != null) {
                factDAO.updateFact(id, customerId, object);
                logger.debug("Updated fact {}:{}:{}", id, customerId, object);
            }
        }

        @Override
        public void objectDeleted(ObjectDeletedEvent event) {
            Object object = event.getOldObject();
            Long id = factHandleMap.get(event.getFactHandle());
            if (object instanceof PersistentFact && id != null) {
                factDAO.removeFact(id, customerId);
                logger.debug("Deleted fact {}:{}:{}", id, customerId, object);
            }
        }
    }
}