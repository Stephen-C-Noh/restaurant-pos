package com.restaurant.pos.order;

import com.restaurant.pos.events.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs only after the order transaction committed: a rollback pushes nothing. The Kafka event is already
 * durable in the outbox by then; the relay kick below only saves it from waiting for the next poll, and it
 * never blocks the request. Each step logs and swallows its own failure.
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
        relay.kick();
    }
}
