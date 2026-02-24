package com.restaurant.pos.staff;

import com.restaurant.pos.user.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String username,
        String fullName,
        Role role,
        String status,
        LocalDateTime createdAt
) {}
