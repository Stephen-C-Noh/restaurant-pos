package com.restaurant.pos.kds;

import com.restaurant.pos.events.ItemStatusChangedEvent;
import com.restaurant.pos.events.OrderCancelledEvent;
import com.restaurant.pos.events.OrderFiredEvent;
import com.restaurant.pos.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Turns order events into {@code /topic/orders} pushes of the CURRENT order, skipping orders the KDS does not show
 * (and orders that no longer exist), so a replayed or duplicated event is harmless. Relies on the single-instance
 * assumption: the simple broker is per JVM, so only the instance that consumed the event reaches its own clients.
 */
@Service
@RequiredArgsConstructor
public class KitchenDisplayService {

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "order-fired-events", groupId = "kds-consumer-group",
            containerFactory = "orderFiredKafkaListenerContainerFactory")
    public void handleOrderFiredEvent(OrderFiredEvent event) {
        push(event.getOrderId());
    }

    @KafkaListener(topics = "item-status-events", groupId = "kds-consumer-group",
            containerFactory = "itemStatusKafkaListenerContainerFactory")
    public void handleItemStatusChanged(ItemStatusChangedEvent event) {
        push(event.getOrderId());
    }

    /** Tells the KDS to drop the ticket: pushes the order only if it is cancelled now. */
    @KafkaListener(topics = "order-cancelled-events", groupId = "kds-consumer-group",
            containerFactory = "orderCancelledKafkaListenerContainerFactory")
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        orderService.findCancelledOrder(event.getOrderId())
                .ifPresent(order -> messagingTemplate.convertAndSend("/topic/orders", order));
    }

    private void push(UUID orderId) {
        orderService.findKitchenOrder(orderId)
                .ifPresent(order -> messagingTemplate.convertAndSend("/topic/orders", order));
    }
}
