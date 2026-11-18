package com.restaurant.pos.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends DomainEvent {
    private UUID orderId;

    public OrderCancelledEvent(UUID orderId) {
        this.orderId = orderId;

        // Set DomainEvent fields
        setEventType("OrderCancelled");
        setAggregateId(orderId.toString());
        setAggregateType("Order");
    }
}
