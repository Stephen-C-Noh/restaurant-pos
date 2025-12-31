package com.restaurant.pos.events;

import com.restaurant.pos.order.OrderItemStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class ItemStatusChangedEvent extends DomainEvent{
    private UUID orderId;
    private String orderNumber;
    private UUID itemId;
    private String menuItemName;
    private OrderItemStatus oldStatus;
    private OrderItemStatus newStatus;
    private Instant changedAt;

    public ItemStatusChangedEvent(UUID orderId, String orderNumber, UUID itemId, String menuItemName, OrderItemStatus oldStatus, OrderItemStatus newStatus) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.itemId = itemId;
        this.menuItemName = menuItemName;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = Instant.now();

        // Set DomainEvent fields
        setEventType("ItemStatusChanged");
        setAggregateId(orderId.toString());
        setAggregateType("Order");
    }
}
