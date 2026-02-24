package com.restaurant.pos.auth;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username,
        String fullName,
        String role
) {}
