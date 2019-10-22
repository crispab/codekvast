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
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.messaging.Event;
import io.codekvast.common.messaging.EventReceiver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Receiver of events from the database table {@code internal_event_queue}.
 *
 * Is activated by the Spring profile {@code event-receiver}.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Profile("event-receiver")
@RequiredArgsConstructor
@Slf4j
public class EventReceiverDAO extends EventDAOBase implements EventReceiver {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final LockManager lockManager;

    private final RowMapper<Event> eventRowMapper = new EventRowMapper();

    @Override
    @Transactional
    public List<Event> getOldestEvents(int max) {
        Optional<LockManager.Lock> lock = lockManager.acquireLock(LockManager.Lock.EVENT_QUEUE);

        if (lock.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events =
            jdbcTemplate.query("SELECT id, createdAt, eventId, correlationId, environment, sendingApp, sendingAppVersion," +
                                   " sendingHostname, type, data " +
                                   "FROM internal_event_queue ORDER BY id LIMIT :max", Map.of("max", max),
                               this.eventRowMapper)
                        .stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (events.isEmpty()) {
            logger.debug("Retrieved no events");
        } else {
            logger.info("Retrieved {} events", events.size());
        }
        return events;
    }

    @Override
    public void acknowledgeEvents(List<Event> events) {
        List<@NonNull Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        int deleted = jdbcTemplate.update("DELETE FROM internal_event_queue WHERE id IN (:ids)", Map.of("ids", ids));
        if (deleted != events.size()) {
            logger.warn("Failed to delete all events. Expected={}, actual={}", events.size(), deleted);
        } else {
            logger.debug("Deleted {} events", deleted);
        }
    }

    private class EventRowMapper implements RowMapper<Event> {
        @Override
        public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
            String type = rs.getString("type");
            String data = rs.getString("data");
            try {
                return Event.builder()
                            .id(rs.getLong("id"))
                            .correlationId(rs.getString("correlationId"))
                            .createdAt(rs.getTimestamp("createdAt").toInstant())
                            .environment(rs.getString("environment"))
                            .eventId(rs.getString("eventId"))
                            .sendingApp(rs.getString("sendingApp"))
                            .sendingAppVersion(rs.getString("sendingAppVersion"))
                            .sendingHostname(rs.getString("sendingHostname"))
                            .data(fromJson(type, data))
                            .build();
            } catch (Exception e) {
                logger.error("Cannot deserialize '{}' as a {}: {}", data, type, e.toString());
            }
            return null;
        }

        private Object fromJson(String type, String data) throws ClassNotFoundException, JsonProcessingException {
            return objectMapper.readValue(data, Class.forName(type));
        }
    }

}
