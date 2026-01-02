package com.restaurant.pos.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status) {
        OrderResponse updated = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/fire")
    public ResponseEntity<OrderResponse> fireOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.fireOrder(id));
    }

    @PatchMapping("/{orderId}/items/{itemId}/status")
    public ResponseEntity<OrderItemResponse> updateItemStatus(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId,
            @RequestBody UpdateItemStatusRequest request){
        OrderItemResponse updated = orderService.updateOrderItemStatus(orderId, itemId, request.getStatus());
        return ResponseEntity.ok(updated);
        /* **Explanation:**
            - `@PatchMapping` - PATCH is used for partial updates (perfect for status changes)
            - `/{orderId}/items/{itemId}/status` - RESTful URL structure
            - Takes both orderId and itemId from URL path
            - Request body contains the new status
            - Returns the updated order item

        **Full endpoint URL will be:**
            PATCH http://localhost:8090/api/orders/{orderId}/items/{itemId}/status

         */
    }
    @GetMapping("/active")
    public ResponseEntity<List<OrderResponse>> getActiveOrders() {
        return(ResponseEntity.ok(orderService.getActiveOrders()));
    }
}