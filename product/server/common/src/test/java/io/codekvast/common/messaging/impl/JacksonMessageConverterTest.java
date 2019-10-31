package io.codekvast.common.messaging.impl;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.messaging.CodekvastMessage;
import io.codekvast.common.messaging.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
class JacksonMessageConverterTest {

    private static final String APPLICATION_NAME = "applicationName";

    private final Instant NOW = Instant.now();

    @Mock
    private CodekvastCommonSettings settings;

    @Mock
    private Clock clock;

    @InjectMocks
    private JacksonMessageConverter converter;

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
        CodekvastMessage codekvastMessage = converter.fromMessage(message);

        // Then
        assertThat(codekvastMessage.getCorrelationId(), notNullValue());
        assertThat(codekvastMessage.getMessageId(), notNullValue());
        assertThat(codekvastMessage.getPayload(), is(sampleEvent));
        assertThat(codekvastMessage.getSenderApp(), is(APPLICATION_NAME));
        assertThat(codekvastMessage.getTimestamp(), is(NOW.truncatedTo(ChronoUnit.MILLIS)));
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
            Arguments.of(AgentPolledAfterTrialPeriodExpiredEvent.sample()),
            Arguments.of(AppDetailsUpdatedEvent.sample()),
            Arguments.of(CodeBaseReceivedEvent.sample()),
            Arguments.of(CollectionStartedEvent.sample()),
            Arguments.of(CustomerAddedEvent.sample()),
            Arguments.of(CustomerDeletedEvent.sample()),
            Arguments.of(InvocationDataReceivedEvent.sample()),
            Arguments.of(PlanChangedEvent.sample()),
            Arguments.of(PlanOverridesDeletedEvent.sample()),
            Arguments.of(TrialPeriodStartedEvent.sample()),
            Arguments.of(UserLoggedInEvent.sample()));
    }
}
