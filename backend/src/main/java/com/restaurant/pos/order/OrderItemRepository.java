package com.restaurant.pos.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);

    @Query(nativeQuery = true, value = """
        SELECT mi.name,
               SUM(oi.quantity)  AS quantity_sold,
               SUM(oi.subtotal)  AS revenue
        FROM order_items oi
        JOIN orders o      ON oi.order_id      = o.id
        JOIN menu_items mi ON oi.menu_item_id  = mi.id
        WHERE o.created_at >= :start AND o.created_at < :end
          AND o.status NOT IN ('DRAFT','CANCELLED')
        GROUP BY mi.name
        ORDER BY quantity_sold DESC
        LIMIT 5
        """)
    List<Object[]> getTopSellingItems(@Param("start") Instant start, @Param("end") Instant end);
}