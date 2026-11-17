package com.restaurant.pos.order;

import com.restaurant.pos.events.EventPublisher;
import com.restaurant.pos.events.ItemStatusChangedEvent;
import com.restaurant.pos.events.OrderFiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Push and publish only after the order transaction committed: a rollback sends nothing,
 * and a slow or dead Kafka can no longer fail the fire or hold its DB connection.
 * Each step logs and swallows its own failure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEvents {

    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderChanged changed) {
        // WS first: the kitchen screen must not wait on Kafka.
        try {
            messagingTemplate.convertAndSend("/topic/orders", changed.order());
        } catch (Exception e) {
            log.error("WebSocket push failed for order {}", changed.order().getId(), e);
        }
        try {
            if (changed.event() instanceof OrderFiredEvent e) {
                eventPublisher.publishOrderFiredEvent(e);
            } else if (changed.event() instanceof ItemStatusChangedEvent e) {
                eventPublisher.publishItemStatusChangedEvent(e);
            }
        } catch (Exception e) {
            log.error("Kafka publish failed for order {}", changed.order().getId(), e);
        }
    }
}
