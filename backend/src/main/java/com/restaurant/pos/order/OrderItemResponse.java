package com.restaurant.pos.order;

import com.restaurant.pos.menu.KitchenSection;
import lombok.Data;
import org.apache.kafka.shaded.com.google.protobuf.Enum;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemResponse {
    private UUID id;
    private UUID menuItemId;
    private String menuItemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String specialInstructions;
    private OrderItemStatus status;
    private KitchenSection section;
}