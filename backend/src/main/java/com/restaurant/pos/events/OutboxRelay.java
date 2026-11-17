package com.restaurant.pos.events;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int BATCH = 50;

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public void drain() throws ExecutionException, InterruptedException {
        for (OutboxRepository.Row row : outbox.pending(BATCH)) {
            kafka.send(row.topic(), row.key(), row.payload()).get();
            outbox.markProcessed(row.id());
        }
    }
}
