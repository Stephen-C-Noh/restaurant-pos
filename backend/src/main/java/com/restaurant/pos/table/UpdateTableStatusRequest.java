package com.restaurant.pos.table;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateTableStatusRequest(
        @NotNull TableStatus status,
        UUID serverId
) {}
