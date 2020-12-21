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
package io.codekvast.backoffice.rules.impl

import io.codekvast.backoffice.facts.ContactDetails
import io.codekvast.backoffice.facts.PersistentFact
import io.codekvast.backoffice.facts.TransientFact
import io.codekvast.backoffice.rules.RuleEngine
import io.codekvast.backoffice.service.MailSender
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.messaging.model.CodekvastEvent
import io.codekvast.common.logging.LoggingUtils
import org.kie.api.KieServices
import org.kie.api.event.rule.ObjectDeletedEvent
import org.kie.api.event.rule.ObjectInsertedEvent
import org.kie.api.event.rule.ObjectUpdatedEvent
import org.kie.api.event.rule.RuleRuntimeEventListener
import org.kie.api.runtime.KieContainer
import org.kie.api.runtime.rule.FactHandle
import org.kie.internal.io.ResourceFactory
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.*
import javax.annotation.PostConstruct

/** @author olle.hallin@crisp.se
 */
@Component
class RuleEngineImpl(
  private val factDAO: FactDAO,
  private val mailSender: MailSender,
  private val customerService: CustomerService,
  private val clock: Clock) : RuleEngine {

  private val logger = LoggerFactory.getLogger(this::class.java.packageName)
  private val droolsLogger = LoggerFactory.getLogger(this::class.java.packageName + ".Drools")
  private lateinit var kieContainer: KieContainer

  private val rulesPath = "rules/"

  @PostConstruct
  fun configureDrools(): RuleEngine {
    val startedAt = clock.instant()
    val kieServices = KieServices.Factory.get()
    val kieFileSystem = kieServices.newKieFileSystem()
    for (file in getRuleFiles()) {
      val resource = ResourceFactory.newClassPathResource(rulesPath + file.filename, "UTF-8")
      logger.debug("Loading rule resource {}", resource)
      kieFileSystem.write(resource)
    }
    val kieRepository = kieServices.repository
    kieRepository.addKieModule { kieRepository.defaultReleaseId }
    val kieBuilder = kieServices.newKieBuilder(kieFileSystem)
    kieBuilder.buildAll()
    kieContainer = kieServices.newKieContainer(kieRepository.defaultReleaseId)
    logger.debug("Configured Drools in {}", LoggingUtils.humanReadableDuration(startedAt, clock.instant()))
    return this
  }

  private fun getRuleFiles(): Array<out org.springframework.core.io.Resource> {
    val resourcePatternResolver: ResourcePatternResolver = PathMatchingResourcePatternResolver()
    return resourcePatternResolver.getResources("classpath*:$rulesPath**/*.*")
  }

  @Transactional(rollbackFor = [Exception::class])
  override fun handle(event: CodekvastEvent) {
    val startedAt = clock.instant()
    val customerId = event.customerId
    val session = kieContainer.newKieSession()
    session.setGlobal("clock", clock)
    session.setGlobal("customerId", customerId)
    session.setGlobal("logger", droolsLogger)
    session.setGlobal("mailSender", mailSender)

    // session.addEventListener(new DebugAgendaEventListener());

    // First load all old facts from the database, and remember their fact handles...
    val factHandleMap: MutableMap<FactHandle, Long> = HashMap()
    for ((id, fact) in factDAO.getFacts(customerId)) {
      val handle = session.insert(fact)
      factHandleMap[handle] = id
    }

    // Add some facts about the customer from the database. These may change anytime, and are not
    // communicated as events.
    for (fact in getTransientFacts(customerId)) {
      session.insert(fact)
    }

    // Add this event as a transient fact...
    session.insert(event)

    // Attach an event listener that will persist all changes caused by fired rules...
    session.addEventListener(PersistentFactEventListener(factHandleMap, customerId))

    // Fire the rules...
    session.fireAllRules()

    // Cleanup...
    session.dispose()
    logger.debug("Rules executed in {}", LoggingUtils.humanReadableDuration(startedAt, clock.instant()))
  }

  private fun getTransientFacts(customerId: Long): List<TransientFact> {
    val result: MutableList<TransientFact> = ArrayList()
    customerService.getCustomerDataByCustomerId(customerId).contactEmail
      ?.takeIf { it.isNotBlank() && !it.trim().startsWith("!") }
      ?.apply { result.add(ContactDetails(contactEmail = this)) }
    return result
  }

  private inner class PersistentFactEventListener(
    private val factHandleMap: MutableMap<FactHandle, Long>,
    private val customerId: Long) : RuleRuntimeEventListener {

    override fun objectInserted(event: ObjectInsertedEvent) {
      val obj = event.getObject()
      if (obj is PersistentFact) {
        val factId = factDAO.addFact(customerId, obj)
        logger.debug("Added fact {}:{}:{}", customerId, factId, obj)
        factHandleMap[event.factHandle] = factId
      }
    }

    override fun objectUpdated(event: ObjectUpdatedEvent) {
      val obj = event.getObject()
      val factId = factHandleMap[event.factHandle]
      if (obj is PersistentFact && factId != null) {
        factDAO.updateFact(customerId, factId, obj)
        logger.debug("Updated fact {}:{}:{}", customerId, factId, obj)
      }
    }

    override fun objectDeleted(event: ObjectDeletedEvent) {
      val obj = event.oldObject
      val factId = factHandleMap[event.factHandle]
      if (obj is PersistentFact && factId != null) {
        factDAO.removeFact(customerId, factId)
        logger.debug("Deleted fact {}:{}:{}", customerId, factId, obj)
      }
    }
  }
}
