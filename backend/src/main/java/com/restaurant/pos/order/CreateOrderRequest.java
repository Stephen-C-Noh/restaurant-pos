package com.restaurant.pos.order;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    private OrderType orderType;
    private UUID tableId;
    private UUID serverId;
    private Integer guestCount;
    private String notes;
    private List<OrderItemRequest> items;
}