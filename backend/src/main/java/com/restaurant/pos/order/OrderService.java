package com.restaurant.pos.order;

import com.restaurant.pos.events.EventPublisher;
import com.restaurant.pos.events.ItemStatusChangedEvent;
import com.restaurant.pos.events.OrderFiredEvent;
import com.restaurant.pos.events.OrderItemEvent;
import com.restaurant.pos.menu.KitchenSection;
import com.restaurant.pos.menu.MenuItem;
import com.restaurant.pos.menu.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuService menuService;
    private final EventPublisher eventPublisher;

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
        response.setFiredAt(order.getFiredAt());

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

    @Transactional
    public OrderResponse fireOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        OrderStatus orderStatus = order.getStatus();
        if(!orderStatus.equals(OrderStatus.DRAFT) && !orderStatus.equals(OrderStatus.SUBMITTED)) {
            throw new IllegalStateException("An order should only be fired when it's in DRAFT or SUBMITTED Current Status: " + orderStatus);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot fire an empty order");
        }

        order.setStatus(OrderStatus.FIRED);

        order.setFiredAt(Instant.now());

        for (OrderItem item : items){
            item.setStatus(OrderItemStatus.FIRED);
            orderItemRepository.save(item);
        }

        orderRepository.save(order);

        // Group items by kitchen section for the event
        Map<KitchenSection, List<OrderItemEvent>> itemsBySection = items.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            MenuItem menuItem = menuService.getMenuItemById(item.getMenuItemId());
                            return menuItem.getKitchenSection();
                        },
                        Collectors.mapping(
                                item -> {
                                    MenuItem menuItem = menuService.getMenuItemById(item.getMenuItemId());
                                    return new OrderItemEvent(
                                            item.getId(),
                                            item.getMenuItemId(),
                                            menuItem.getName(),
                                            item.getQuantity(),
                                            item.getUnitPrice(),
                                            item.getSpecialInstructions()
                                    );
                                },
                                Collectors.toList()
                        )
                ));

// Create and publish the event
        OrderFiredEvent event = new OrderFiredEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getFiredAt(),
                itemsBySection
        );

        eventPublisher.publishOrderFiredEvent(event);
        /*
        If we wrote this without streams, it would look like:

        Map<KitchenSection, List<OrderItemEvent>> itemsBySection = new HashMap<>();

        for (OrderItem item : items) {
            // Get the menu item
            MenuItem menuItem = menuService.getMenuItemById(item.getMenuItemId());
            KitchenSection section = menuItem.getKitchenSection();

            // Create the event object
            OrderItemEvent eventItem = new OrderItemEvent(
                item.getId(),
                item.getMenuItemId(),
                menuItem.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSpecialInstructions()
            );

            // Add to the map
            if (!itemsBySection.containsKey(section)) {
                itemsBySection.put(section, new ArrayList<>());
            }
            itemsBySection.get(section).add(eventItem);
        }
        */
        return toOrderResponse(order);
    }

    @Transactional
    public OrderItemResponse updateOrderItemStatus(UUID orderId, UUID itemId, OrderItemStatus newStatus) {
        // Verify order exists
        Order order = orderRepository.findById(orderId). orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Find the specific item
        OrderItem item = orderItemRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        // Verify item belongs to this order
        if(!item.getOrderId().equals(orderId)) {
            throw new IllegalStateException("Item(" + itemId + ") does not belong to Order: " + orderId);
        }

        // Validate status transition( Simple validation for now)
        OrderItemStatus currentStatus = item.getStatus();
        if(!isValidStatusTransition(currentStatus, newStatus)){
            throw new IllegalStateException("Invalid status transition from " + currentStatus+ " to " + newStatus);
        }

        // Update status
        item.setStatus(newStatus);
        orderItemRepository.save(item);

        log.info("Updated item {} status: {} -> {}", itemId, currentStatus, newStatus);

        // Get menu item name for the event
        MenuItem menuItem = menuService.getMenuItemById(item.getMenuItemId());

        // publish status change event
        ItemStatusChangedEvent event = new ItemStatusChangedEvent(
                orderId,
                order.getOrderNumber(),
                itemId,
                menuItem.getName(),
                currentStatus,
                newStatus
        );

        eventPublisher.publishItemStatusChangedEvent(event);

        return toOrderItemResponse(item);
    }

    private boolean isValidStatusTransition(OrderItemStatus currentStatus, OrderItemStatus newStatus) {
        // Define valid transitions
        // PENDING -> FIRED -> PREPARING -> READY -> SERVED

        if(currentStatus.equals(newStatus)){ return true; }

        switch(currentStatus){
            case PENDING:
                return newStatus.equals(OrderItemStatus.FIRED);
            case FIRED:
                return newStatus.equals(OrderItemStatus.PREPARING);
            case PREPARING:
                return newStatus.equals(OrderItemStatus.READY);
            case READY:
                return newStatus.equals(OrderItemStatus.SERVED);
            default:
                return false;
        }
    }
}