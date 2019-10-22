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
import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.messaging.EventSender;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An implementation of Messaging service that uses the database table internal_event_queue as queue.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventSenderDAO extends EventDAOBase implements EventSender {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final CodekvastCommonSettings settings;

    @Override
    @Transactional
    public void publish(@NonNull Object event) {
        publish(event, UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public void publish(@NonNull Object event, @NonNull String correlationId) {
        Map<String, Object> params = new HashMap<>();
        params.put("createdAt", Timestamp.from(clock.instant()));
        params.put("eventId", UUID.randomUUID().toString());
        params.put("correlationId", correlationId);
        params.put("environment", settings.getEnvironment());
        params.put("sendingApp", settings.getApplicationName());
        params.put("sendingAppVersion", settings.getDisplayVersion());
        params.put("sendingHostname", settings.getDnsCname());
        params.put("type", event.getClass().getName());
        try {
            params.put("data", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot convert " + event + " to JSON", e);
        }

        jdbcTemplate.update(
            "INSERT INTO internal_event_queue(createdAt, eventId, correlationId, environment, sendingApp, sendingAppVersion, " +
                "sendingHostname, type, data) \n" +
                "VALUES (:createdAt, :eventId, :correlationId, :environment, :sendingApp, :sendingAppVersion, :sendingHostname, :type, " +
                ":data)",
            params);
        logger.debug("Appended {} to internal_event_queue", event);
    }

}
