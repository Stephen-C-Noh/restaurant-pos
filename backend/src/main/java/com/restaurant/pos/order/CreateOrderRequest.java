package com.restaurant.pos.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    @NotNull(message = "orderType is required")
    private OrderType orderType;
    private UUID tableId;
    private UUID serverId;
    private Integer guestCount;
    private String notes;
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;
}