/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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

import static io.codekvast.common.util.LoggingUtils.humanReadableDuration;

import io.codekvast.backoffice.facts.ContactDetails;
import io.codekvast.backoffice.facts.PersistentFact;
import io.codekvast.backoffice.facts.TransientFact;
import io.codekvast.backoffice.rules.RuleEngine;
import io.codekvast.backoffice.service.MailSender;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.messaging.model.CodekvastEvent;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** @author olle.hallin@crisp.se */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEngineImpl implements RuleEngine {
  private static final String RULES_PATH = "rules/";

  private final FactDAO factDAO;
  private final MailSender mailSender;
  private final CustomerService customerService;
  private final Clock clock;

  private final Logger droolsLogger =
      LoggerFactory.getLogger(RuleEngine.class.getPackageName() + ".Drools");

  private KieContainer kieContainer;

  @PostConstruct
  public RuleEngine configureDrools() throws IOException {
    Instant startedAt = clock.instant();
    KieServices kieServices = KieServices.Factory.get();
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
    for (Resource file : getRuleFiles()) {
      val resource = ResourceFactory.newClassPathResource(RULES_PATH + file.getFilename(), "UTF-8");
      logger.debug("Loading rule resource {}", resource);
      kieFileSystem.write(resource);
    }
    KieRepository kieRepository = kieServices.getRepository();
    kieRepository.addKieModule(kieRepository::getDefaultReleaseId);

    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.buildAll();
    kieContainer = kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
    logger.debug("Configured Drools in {}", humanReadableDuration(startedAt, clock.instant()));
    return this;
  }

  private Resource[] getRuleFiles() throws IOException {
    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    return resourcePatternResolver.getResources("classpath*:" + RULES_PATH + "**/*.*");
  }

  @Override
  @Transactional
  public void handle(CodekvastEvent event) {
    Instant startedAt = clock.instant();
    final Long customerId = event.getCustomerId();

    KieSession session = kieContainer.newKieSession();
    session.setGlobal("clock", clock);
    session.setGlobal("customerId", customerId);
    session.setGlobal("logger", droolsLogger);
    session.setGlobal("mailSender", mailSender);

    // session.addEventListener(new DebugAgendaEventListener());

    // First load all old facts from the database, and remember their fact handles...
    Map<FactHandle, Long> factHandleMap = new HashMap<>();
    for (FactWrapper w : factDAO.getFacts(customerId)) {
      FactHandle handle = session.insert(w.getFact());
      factHandleMap.put(handle, w.getId());
    }

    // Add some facts about the customer from the database. These may change anytime, and are not
    // communicated as events.
    for (TransientFact fact : getTransientFacts(customerId)) {
      session.insert(fact);
    }

    // Add this event as a transient fact...
    session.insert(event);

    // Attach an event listener that will persist all changes caused by fired rules...
    session.addEventListener(new PersistentFactEventListener(factHandleMap, customerId));

    // Fire the rules...
    session.fireAllRules();

    // Cleanup...
    session.dispose();

    logger.debug("Rules executed in {}", humanReadableDuration(startedAt, clock.instant()));
  }

  private List<TransientFact> getTransientFacts(Long customerId) {
    List<TransientFact> result = new ArrayList<>();

    Optional.ofNullable(customerService.getCustomerDataByCustomerId(customerId).getContactEmail())
        .map(String::trim)
        .filter(s -> !s.isEmpty() && !s.startsWith("!"))
        .ifPresent(s -> result.add(ContactDetails.builder().contactEmail(s).build()));

    return result;
  }

  @RequiredArgsConstructor
  private class PersistentFactEventListener implements RuleRuntimeEventListener {
    private final Map<FactHandle, Long> factHandleMap;
    private final Long customerId;

    @Override
    public void objectInserted(ObjectInsertedEvent event) {
      Object object = event.getObject();
      if (object instanceof PersistentFact) {
        Long factId = factDAO.addFact(customerId, (PersistentFact) object);
        logger.debug("Added fact {}:{}:{}", customerId, factId, object);
        factHandleMap.put(event.getFactHandle(), factId);
      }
    }

    @Override
    public void objectUpdated(ObjectUpdatedEvent event) {
      Object object = event.getObject();
      Long factId = factHandleMap.get(event.getFactHandle());
      if (object instanceof PersistentFact && factId != null) {
        factDAO.updateFact(customerId, factId, (PersistentFact) object);
        logger.debug("Updated fact {}:{}:{}", customerId, factId, object);
      }
    }

    @Override
    public void objectDeleted(ObjectDeletedEvent event) {
      Object object = event.getOldObject();
      Long factId = factHandleMap.get(event.getFactHandle());
      if (object instanceof PersistentFact && factId != null) {
        factDAO.removeFact(customerId, factId);
        logger.debug("Deleted fact {}:{}:{}", customerId, factId, object);
      }
    }
  }
}
