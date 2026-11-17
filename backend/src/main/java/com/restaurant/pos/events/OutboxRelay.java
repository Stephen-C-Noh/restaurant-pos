package com.restaurant.pos.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final int BATCH = 50;
    /** The rows are locked while we wait, so wait far less than Kafka's 120 s delivery timeout. */
    private static final long ACK_SECONDS = 5;

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;

    // One thread runs every drain from kick(), so drains never overlap inside this JVM and a request thread
    // never waits on Kafka.
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "outbox-relay");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean drainQueued = new AtomicBoolean();

    /** Never blocks the caller. Calls while a drain is already queued merge into it. */
    public void kick() {
        if (drainQueued.compareAndSet(false, true)) {
            executor.execute(() -> {
                drainQueued.set(false);
                try {
                    drain();
                } catch (Exception e) {
                    log.error("Outbox drain failed", e);
                }
            });
        }
    }

    /** One batch in one transaction, so the rows stay locked for other drains until it commits. */
    public void drain() {
        tx.executeWithoutResult(status -> {
            for (OutboxRepository.Row row : outbox.lockPending(BATCH)) {
                try {
                    kafka.send(row.topic(), row.key(), row.payload()).get(ACK_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Outbox publish to {} failed for {}", row.topic(), row.key(), e);
                    outbox.markFailed(row.id(), e.toString());
                    return; // later rows must not overtake this one
                }
                outbox.markProcessed(row.id());
            }
        });
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
