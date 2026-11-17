package com.restaurant.pos.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    /** The mapper Kafka's JsonSerializer used before the outbox, so the KDS consumers read the same JSON. */
    private static final ObjectMapper JSON = JacksonUtils.enhancedObjectMapper();

    private final JdbcTemplate jdbc;

    /** Joins the caller's transaction: the row commits or rolls back with the order change. */
    public void add(DomainEvent event, String topic) {
        String payload;
        try {
            payload = JSON.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + event.getEventType(), e);
        }
        jdbc.update("""
                INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, topic)
                VALUES (?, ?, ?, ?::jsonb, ?)""",
                event.getAggregateType(), event.getAggregateId(), event.getEventType(), payload, topic);
    }
}
