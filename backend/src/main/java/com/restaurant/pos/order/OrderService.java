package com.restaurant.pos.order;

import com.restaurant.pos.menu.MenuItem;
import com.restaurant.pos.menu.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuService menuService;

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Create order
        Order order = new Order();
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setOrderType(request.getOrderType());
        order.setStatus(OrderStatus.DRAFT);
        order.setTableId(request.getTableId());
        order.setServerId(request.getServerId());
        order.setGuestCount(request.getGuestCount());
        order.setNotes(request.getNotes());

        // Save order first to get ID
        order = orderRepository.save(order);

        // Create order items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            BigDecimal orderSubtotal = BigDecimal.ZERO;

            for (OrderItemRequest itemRequest : request.getItems()) {
                MenuItem menuItem = menuService.getMenuItemById(itemRequest.getMenuItemId());

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setMenuItemId(menuItem.getId());
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setUnitPrice(menuItem.getBasePrice());

                // Calculate item subtotal
                BigDecimal itemSubtotal = menuItem.getBasePrice()
                        .multiply(new BigDecimal(itemRequest.getQuantity()));
                orderItem.setSubtotal(itemSubtotal);
                orderItem.setSpecialInstructions(itemRequest.getSpecialInstructions());

                orderItemRepository.save(orderItem);

                orderSubtotal = orderSubtotal.add(itemSubtotal);
            }

            // Calculate order totals
            order.setSubtotal(orderSubtotal);

            // Calculate tax (5%)
            BigDecimal taxAmount = orderSubtotal
                    .multiply(new BigDecimal("0.05"))
                    .setScale(2, RoundingMode.HALF_UP);
            order.setTaxAmount(taxAmount);

            // Calculate total
            BigDecimal total = orderSubtotal.add(taxAmount);
            order.setTotal(total);

            order = orderRepository.save(order);
        }

        // Fetch fresh copy with timestamps
        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        order.setStatus(newStatus);
        orderRepository.save(order);
        return toOrderResponse(order);
    }

    // Helper method to convert Order to OrderResponse with items
    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderType(order.getOrderType());
        response.setStatus(order.getStatus());
        response.setTableId(order.getTableId());
        response.setServerId(order.getServerId());
        response.setGuestCount(order.getGuestCount());
        response.setSubtotal(order.getSubtotal());
        response.setTaxAmount(order.getTaxAmount());
        response.setTotal(order.getTotal());
        response.setNotes(order.getNotes());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // Load items
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setMenuItemId(item.getMenuItemId());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());
        response.setSpecialInstructions(item.getSpecialInstructions());
        response.setStatus(item.getStatus());

        // Get menu item name
        try {
            MenuItem menuItem = menuService.getMenuItemById(item.getMenuItemId());
            response.setMenuItemName(menuItem.getName());
        } catch (Exception e) {
            response.setMenuItemName("Unknown Item");
        }

        return response;
    }
}