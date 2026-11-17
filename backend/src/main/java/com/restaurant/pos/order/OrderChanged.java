package com.restaurant.pos.order;

import com.restaurant.pos.events.DomainEvent;

/** Published inside the order transaction; {@link OrderEvents} acts on it only after the commit. */
public record OrderChanged(DomainEvent event, OrderResponse order) {
}
