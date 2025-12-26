package com.restaurant.pos.order;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderItemRequest {
    private UUID menuItemId;
    private Integer quantity;
    private String specialInstructions;
}