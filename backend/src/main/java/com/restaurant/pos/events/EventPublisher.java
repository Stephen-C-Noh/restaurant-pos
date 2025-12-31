package com.restaurant.pos.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
// Spring's tool for sending messages to Kafka

import org.springframework.stereotype.Service;




@Service
//@Service - Spring Component Stereotype
//This tells Spring: "This class is a service - manage it for me!"
//What Spring does:
//
// Creates ONE instance of this class when the app starts (Singleton)
// Stores it in the "Spring Container" (like a registry of objects)
// Automatically injects it wherever needed (Dependency Injection)
//
//Without @Service:
//java// You'd have to manually create instances everywhere
//EventPublisher publisher = new EventPublisher(kafkaTemplate);
//publisher.publishOrderFiredEvent(event);
//With @Service:
//java@Service
//public class OrderService {
//    private final EventPublisher eventPublisher;  // Spring injects this automatically!
//
//    // Spring finds EventPublisher in its container and gives it to you
//}

@RequiredArgsConstructor
// This generates a constructor with parameters for all final fields.

@Slf4j
// Lombok annotation that gives you a logger automatically

public class EventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    private static final String ORDER_FIRED_TOPIC = "order-fired-events";        // ← Separate topic!
    private static final String ITEM_STATUS_TOPIC = "item-status-events";        // ← Separate topic!
    // ORDER_EVENTS_TOPIC - The Kafka topic name where events go

    public void publishOrderFiredEvent(OrderFiredEvent event) {
        // publishOrderFiredEvent() - Sends the event to Kafka
        try {
            log.info("Publishing OrderFiredEvent for order: {}", event.getOrderNumber());
            kafkaTemplate.send(ORDER_FIRED_TOPIC, event.getOrderId().toString(), event);  // ← New topic
            log.info("Successfully published OrderFiredEvent for order: {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish OrderFiredEvent for order: {}", event.getOrderNumber(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    public void publishItemStatusChangedEvent(ItemStatusChangedEvent event) {
        try {
            log.info("Publishing ItemStatusChangedEvent for item: {} ({} -> {})",
                    event.getMenuItemName(), event.getOldStatus(), event.getNewStatus());
            kafkaTemplate.send(ITEM_STATUS_TOPIC, event.getOrderId().toString(), event);  // ← New topic
            log.info("Successfully published ItemStatusChangedEvent for item: {}", event.getMenuItemName());
        } catch (Exception e) {
            log.error("Failed to publish ItemStatusChangedEvent for item: {}", event.getMenuItemName(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

}
