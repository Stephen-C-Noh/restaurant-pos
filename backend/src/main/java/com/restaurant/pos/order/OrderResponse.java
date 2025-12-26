package com.restaurant.pos.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private OrderType orderType;
    private OrderStatus status;
    private UUID tableId;
    private UUID serverId;
    private Integer guestCount;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String notes;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant firedAt;
}