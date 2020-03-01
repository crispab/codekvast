package io.codekvast.common.messaging.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.messaging.model.AgentPolledEvent;
import io.codekvast.common.messaging.model.AppDetailsUpdatedEvent;
import io.codekvast.common.messaging.model.CodeBaseReceivedEvent;
import io.codekvast.common.messaging.model.CodekvastEvent;
import io.codekvast.common.messaging.model.CollectionStartedEvent;
import io.codekvast.common.messaging.model.CustomerAddedEvent;
import io.codekvast.common.messaging.model.CustomerDeletedEvent;
import io.codekvast.common.messaging.model.InvocationDataReceivedEvent;
import io.codekvast.common.messaging.model.LicenseViolationEvent;
import io.codekvast.common.messaging.model.PlanChangedEvent;
import io.codekvast.common.messaging.model.PlanOverridesDeletedEvent;
import io.codekvast.common.messaging.model.UserAuthenticatedEvent;
import io.codekvast.common.messaging.model.UserLoggedInEvent;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;

/** @author olle.hallin@crisp.se */
class CodekvastJsonMessageConverterTest {

  private static final String APPLICATION_NAME = "applicationName";

  private final Instant NOW = Instant.now();

  @Mock private CodekvastCommonSettings settings;

  @Mock private Clock clock;

  @InjectMocks private CodekvastJsonMessageConverter converter;

  @BeforeEach
  void beforeEach() {
    MockitoAnnotations.initMocks(this);
    when(settings.getApplicationName()).thenReturn(APPLICATION_NAME);
    when(clock.instant()).thenReturn(NOW);
  }

  @ParameterizedTest
  @MethodSource("provideCodekvastEventSamples")
  void should_serialize_and_deserialize_event(CodekvastEvent sampleEvent) {
    // Given

    // When
    Message message = converter.toMessage(sampleEvent, null);
    Object fromMessage = converter.fromMessage(message);

    // Then
    assertThat(fromMessage, is(sampleEvent));
  }

  @Test
  void should_produce_camel_case_JSON() {
    // Given
    Message message = converter.toMessage(CustomerAddedEvent.sample(), null);

    // When
    String json = new String(message.getBody(), StandardCharsets.UTF_8);

    // Then
    assertThat(json, matchesPattern(".*\"customerId\":1,.*"));
  }

  private static Stream<Arguments> provideCodekvastEventSamples() {
    return Stream.of(
        Arguments.of(AgentPolledEvent.sample()),
        Arguments.of(AppDetailsUpdatedEvent.sample()),
        Arguments.of(CodeBaseReceivedEvent.sample()),
        Arguments.of(CollectionStartedEvent.sample()),
        Arguments.of(CustomerAddedEvent.sample()),
        Arguments.of(CustomerDeletedEvent.sample()),
        Arguments.of(InvocationDataReceivedEvent.sample()),
        Arguments.of(LicenseViolationEvent.sample()),
        Arguments.of(PlanChangedEvent.sample()),
        Arguments.of(PlanOverridesDeletedEvent.sample()),
        Arguments.of(UserAuthenticatedEvent.sample()),
        Arguments.of(UserLoggedInEvent.sample()));
  }
}
