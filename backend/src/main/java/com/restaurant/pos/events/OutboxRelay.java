package com.restaurant.pos.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final int BATCH = 50;

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public void drain() {
        for (OutboxRepository.Row row : outbox.pending(BATCH)) {
            try {
                kafka.send(row.topic(), row.key(), row.payload()).get();
            } catch (Exception e) {
                log.warn("Outbox publish to {} failed for {}", row.topic(), row.key(), e);
                outbox.markFailed(row.id(), e.toString());
                continue;
            }
            outbox.markProcessed(row.id());
        }
    }
}
