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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.common.messaging.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.messaging.CodekvastMessage;
import io.codekvast.common.messaging.CorrelationIdHolder;
import io.codekvast.common.messaging.model.CodekvastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.UUID;

/**
 * An AMQP message converter that uses Jackson for serializing to/from JSON.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JacksonMessageConverter implements MessageConverter {

    private final CodekvastCommonSettings settings;
    private final Clock clock;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public Message toMessage(Object object, MessageProperties messagePropertiesArg) throws MessageConversionException {
        logger.debug("Converting {} to JSON", object);

        MessageProperties messageProperties = messagePropertiesArg;
        if (messageProperties == null) {
            messageProperties = new MessageProperties();
        }

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(object);

            messageProperties.setAppId(settings.getApplicationName());
            messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
            messageProperties.setContentLength(bytes.length);
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            messageProperties.setCorrelationId(CorrelationIdHolder.get());
            messageProperties.setMessageId(UUID.randomUUID().toString());
            messageProperties.setType(object.getClass().getName());
            messageProperties.setTimestamp(Date.from(clock.instant()));
            return new Message(bytes, messageProperties);
        } catch (JsonProcessingException e) {
            throw new MessageConversionException("Cannot convert to JSON", e);
        }
    }

    @Override
    public CodekvastMessage fromMessage(Message message) throws MessageConversionException {
        MessageProperties messageProperties = message.getMessageProperties();

        try {
            CodekvastEvent payload = (CodekvastEvent) objectMapper.readValue(message.getBody(), Class.forName(messageProperties.getType()));
            logger.debug("Converted {} from JSON", payload);
            return CodekvastMessage.builder()
                                   .correlationId(messageProperties.getCorrelationId())
                                   .messageId(messageProperties.getMessageId())
                                   .senderApp(messageProperties.getAppId())
                                   .timestamp(messageProperties.getTimestamp().toInstant())
                                   .payload(payload)
                                   .build();

        } catch (IOException | ClassNotFoundException e) {
            throw new MessageConversionException("Cannot convert from JSON", e);
        }
    }
}
