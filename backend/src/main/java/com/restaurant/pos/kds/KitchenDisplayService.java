package com.restaurant.pos.kds;

import com.restaurant.pos.events.ItemStatusChangedEvent;
import com.restaurant.pos.events.OrderFiredEvent;
import com.restaurant.pos.events.OrderItemEvent;
import com.restaurant.pos.menu.KitchenSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j

/* Explanation:

@KafkaListener - Tells Spring to listen to the "order-events" topic
groupId = "kds-consumer-group" - Consumer group ID (allows scaling)
handleOrderFiredEvent() - Called automatically when a message arrives
displayOrderForSection() - Formats and logs items for each station

 */

public class KitchenDisplayService {

    // Listen to order-fired-events topic
    @KafkaListener(topics = "order-fired-events", groupId = "kds-consumer-group", containerFactory = "orderFiredKafkaListenerContainerFactory")
    public void handleOrderFiredEvent(OrderFiredEvent event) {
        log.info("========================================");
        log.info("ORDER FIRED: {}", event.getOrderNumber());
        log.info("Order ID: {}", event.getOrderId());
        log.info("Fired At: {}", event.getFiredAt());
        log.info("========================================");

        Map<KitchenSection, List<OrderItemEvent>> itemsBySection = event.getItemsBySection();

        for (Map.Entry<KitchenSection, List<OrderItemEvent>> entry : itemsBySection.entrySet()) {
            KitchenSection section = entry.getKey();
            List<OrderItemEvent> items = entry.getValue();
            displayOrderForSection(event.getOrderNumber(), section, items);
        }
    }

    // Listen to item-status-events topic
    @KafkaListener(topics = "item-status-events", groupId = "kds-consumer-group", containerFactory = "itemStatusKafkaListenerContainerFactory")
    public void handleItemStatusChanged(ItemStatusChangedEvent event) {
        log.info("========================================");
        log.info("ITEM STATUS CHANGED");
        log.info("Order: {}", event.getOrderNumber());
        log.info("Item: {}", event.getMenuItemName());
        log.info("Status: {} -> {}", event.getOldStatus(), event.getNewStatus());
        log.info("========================================");
    }

    private void displayOrderForSection(String orderNumber, KitchenSection section, List<OrderItemEvent> items) {
        log.info("");
        log.info(">>> {} STATION <<<", section);
        log.info("Order: {}", orderNumber);

        for (OrderItemEvent item : items) {
            String instructions = item.getSpecialInstructions() != null
                    ? "(" + item.getSpecialInstructions() + ")"
                    : "";
            log.info(" - {} x {} {}", item.getQuantity(), item.getMenuItemName(), instructions);
        }

        log.info("");
    }
}