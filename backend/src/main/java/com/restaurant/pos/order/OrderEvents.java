package com.restaurant.pos.order;

import com.restaurant.pos.events.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Push and publish only after the order transaction committed: a rollback sends nothing,
 * and a slow or dead Kafka can no longer fail the fire. It still delays the response by up to
 * max.block.ms (1 s, KafkaConfig), during which open-in-view keeps the request's DB connection.
 * Each step logs and swallows its own failure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEvents {

    private final SimpMessagingTemplate messagingTemplate;
    private final OutboxRelay relay;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderChanged changed) {
        // WS first: the kitchen screen must not wait on Kafka.
        try {
            messagingTemplate.convertAndSend("/topic/orders", changed.order());
        } catch (Exception e) {
            log.error("WebSocket push failed for order {}", changed.order().getId(), e);
        }
        try {
            relay.drain();
        } catch (Exception e) {
            log.error("Outbox drain failed for order {}", changed.order().getId(), e);
        }
    }
}
