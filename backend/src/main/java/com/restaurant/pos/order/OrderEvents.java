package com.restaurant.pos.order;

import com.restaurant.pos.events.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs only after the order transaction committed: a rollback publishes nothing. The Kafka event is already
 * durable in the outbox by then; the relay kick below only saves it from waiting for the next poll, and it
 * never blocks the request. The kitchen screen is pushed by the Kafka consumer, not from here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEvents {

    private final OutboxRelay relay;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderChanged changed) {
        relay.kick();
    }
}
