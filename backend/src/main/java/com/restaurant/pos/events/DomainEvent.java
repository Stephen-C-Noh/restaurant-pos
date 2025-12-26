package com.restaurant.pos.events;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class DomainEvent {
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private Instant timestamp = Instant.now();
    private int version = 1;
}
