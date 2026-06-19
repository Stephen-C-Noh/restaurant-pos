package com.restaurant.pos.table;

import java.time.LocalDateTime;
import java.util.UUID;

public record TableResponse(
        UUID id,
        String tableNumber,
        int capacity,
        String zone,
        TableStatus status,
        UUID currentServerId,
        String serverName,
        LocalDateTime updatedAt
) {}
