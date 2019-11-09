package io.codekvast.backoffice.rules.impl;

import io.codekvast.backoffice.facts.CollectionStarted;
import io.codekvast.backoffice.rules.RuleEngine;
import io.codekvast.backoffice.service.MailSender;
import io.codekvast.common.messaging.model.CollectionStartedEvent;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
public class RuleEngineImplTest {

    private static final Instant NOW = Instant.now();

    @Mock
    private FactDAO factDAO;

    @Mock
    private Clock clock;

    @Mock
    private MailSender mailSender;

    private RuleEngine ruleEngine;

    @Before
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        when(clock.instant()).thenReturn(NOW);

        ruleEngine = new RuleEngineImpl(factDAO, mailSender, clock);
    }

    @Test
    public void should_send_welcome_email_when_collection_starts() {
        // given
        CollectionStartedEvent event = CollectionStartedEvent.sample();
        Long customerId = event.getCustomerId();
        Long factId = 4711L;
        when(factDAO.getFacts(customerId)).thenReturn(Collections.emptyList());
        when(factDAO.addFact(eq(customerId), any(CollectionStarted.class))).thenReturn(factId);

        // when
        ruleEngine.handle(event);

        // then
        // verify(factDAO).updateFact(eq(factId), eq(customerId), eq(new CollectionStarted(event.getCollectionStartedAt(), event.getTrialPeriodEndsAt(), false)));
        verify(mailSender).sendMail("welcome-collection-has-started", customerId);
        verify(factDAO).updateFact(eq(factId), eq(customerId), eq(new CollectionStarted(event.getCollectionStartedAt(), event.getTrialPeriodEndsAt(), true)));
    }
}
