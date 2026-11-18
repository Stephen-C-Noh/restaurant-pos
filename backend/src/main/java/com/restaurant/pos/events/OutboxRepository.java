package com.restaurant.pos.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    /** The mapper Kafka's JsonSerializer used before the outbox, so the KDS consumers read the same JSON. */
    private static final ObjectMapper JSON = JacksonUtils.enhancedObjectMapper();

    public record Row(UUID id, String topic, String key, String payload) {
    }

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

    /** Oldest pending rows, locked until the caller's transaction ends; rows another drain holds are skipped. */
    public List<Row> lockPending(int limit) {
        return jdbc.query("""
                SELECT id, topic, aggregate_id, payload::text FROM outbox_events
                WHERE status = 'PENDING' ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED""",
                (rs, i) -> new Row(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4)),
                limit);
    }

    public void markProcessed(UUID id) {
        jdbc.update("UPDATE outbox_events SET status = 'PROCESSED', processed_at = now() WHERE id = ?", id);
    }

    public void markFailed(UUID id, String error) {
        jdbc.update("UPDATE outbox_events SET attempts = attempts + 1, last_error = ? WHERE id = ?", error, id);
    }
}
