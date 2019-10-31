package io.codekvast.common.messaging.impl;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
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
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
class JacksonMessageConverterTest {

    private static final String APPLICATION_NAME = "applicationName";

    @Mock
    private CodekvastCommonSettings settings;

    @InjectMocks
    private JacksonMessageConverter converter;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.initMocks(this);
        when(settings.getApplicationName()).thenReturn(APPLICATION_NAME);
    }

    @ParameterizedTest
    @MethodSource("provideCodekvastEventSamples")
    void should_serialize_and_deserialize_event(CodekvastEvent sampleEvent) {
        // Given

        // When
        Message message = converter.toMessage(sampleEvent, null);
        Object deserialized = converter.fromMessage(message);
        MessageProperties messageProperties = message.getMessageProperties();

        // Then
        assertThat(deserialized, is(sampleEvent));
        assertThat(messageProperties, notNullValue());
        assertThat(messageProperties.getCorrelationId(), notNullValue());
        assertThat(messageProperties.getMessageId(), notNullValue());
        assertThat(messageProperties.getAppId(), is(APPLICATION_NAME));
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
            Arguments.of(CollectionStartedEvent.sample()),
            Arguments.of(CustomerAddedEvent.sample()),
            Arguments.of(CustomerDeletedEvent.sample()),
            Arguments.of(PlanChangedEvent.sample()),
            Arguments.of(PlanOverridesDeletedEvent.sample()),
            Arguments.of(TrialPeriodStartedEvent.sample()),
            Arguments.of(UserLoggedInEvent.sample()));
    }
}
