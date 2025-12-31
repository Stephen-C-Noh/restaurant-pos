package com.restaurant.pos.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing domain events to Kafka topics.
 * Handles event serialization and error logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    private static final String ORDER_FIRED_TOPIC = "order-fired-events";
    private static final String ITEM_STATUS_TOPIC = "item-status-events";

    /**
     * Publishes an OrderFiredEvent when an order is sent to the kitchen.
     *
     * @param event The order fired event containing order and item details
     * @throws RuntimeException if publishing fails
     */
    public void publishOrderFiredEvent(OrderFiredEvent event) {
        try {
            log.info("Publishing OrderFiredEvent for order: {}", event.getOrderNumber());
            kafkaTemplate.send(ORDER_FIRED_TOPIC, event.getOrderId().toString(), event);
            log.info("Successfully published OrderFiredEvent for order: {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish OrderFiredEvent for order: {}", event.getOrderNumber(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publishes an ItemStatusChangedEvent when an order item's status is updated.
     *
     * @param event The status change event containing old and new status
     * @throws RuntimeException if publishing fails
     */
    public void publishItemStatusChangedEvent(ItemStatusChangedEvent event) {
        try {
            log.info("Publishing ItemStatusChangedEvent for item: {} ({} -> {})",
                    event.getMenuItemName(), event.getOldStatus(), event.getNewStatus());
            kafkaTemplate.send(ITEM_STATUS_TOPIC, event.getOrderId().toString(), event);
            log.info("Successfully published ItemStatusChangedEvent for item: {}", event.getMenuItemName());
        } catch (Exception e) {
            log.error("Failed to publish ItemStatusChangedEvent for item: {}", event.getMenuItemName(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}