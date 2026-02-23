package com.restaurant.pos.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByStatus(OrderStatus status);
    long countByCreatedAtAfter(Instant timestamp);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    long countByStatusIn(Collection<OrderStatus> statuses);

    @Query(nativeQuery = true, value = """
        SELECT EXTRACT(HOUR FROM created_at) AS hour, COUNT(*) AS order_count
        FROM orders
        WHERE created_at >= :start AND created_at < :end
          AND status NOT IN ('DRAFT','CANCELLED')
        GROUP BY hour ORDER BY hour
        """)
    List<Object[]> getOrdersPerHour(@Param("start") Instant start, @Param("end") Instant end);

    @Query(nativeQuery = true, value = """
        SELECT COALESCE(SUM(total), 0)        AS revenue,
               COUNT(*)                        AS order_count,
               COALESCE(AVG(total), 0)         AS avg_order_value,
               COALESCE((
                 SELECT AVG(table_total) FROM (
                   SELECT table_id, SUM(total) AS table_total
                   FROM orders
                   WHERE created_at >= :start AND created_at < :end
                     AND status NOT IN ('DRAFT','CANCELLED')
                     AND table_id IS NOT NULL
                   GROUP BY table_id
                 ) t
               ), 0)                           AS avg_revenue_per_table
        FROM orders
        WHERE created_at >= :start AND created_at < :end
          AND status NOT IN ('DRAFT','CANCELLED')
        """)
    List<Object[]> getDailyStats(@Param("start") Instant start, @Param("end") Instant end);
}


