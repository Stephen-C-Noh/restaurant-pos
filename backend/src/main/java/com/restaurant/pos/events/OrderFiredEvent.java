package com.restaurant.pos.events;

import com.restaurant.pos.menu.KitchenSection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderFiredEvent extends DomainEvent {
    private UUID orderId;
    private String orderNumber;
    private Instant firedAt;
    private Map<KitchenSection, List<OrderItemEvent>> itemsBySection;

    public OrderFiredEvent(UUID orderId, String orderNumber, Instant firedAt, Map<KitchenSection, List<OrderItemEvent>> itemsBySection) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.firedAt = firedAt;
        this.itemsBySection = itemsBySection;

        // Set DomainEvent fields
        setEventType("OrderFired");
        setAggregateId(orderId.toString());
        setAggregateType("Order");
    }

}
