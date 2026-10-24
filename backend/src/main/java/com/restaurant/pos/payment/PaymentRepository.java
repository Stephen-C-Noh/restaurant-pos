package com.restaurant.pos.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.orderId = :orderId AND p.status IN :statuses")
    BigDecimal sumAmount(@Param("orderId") UUID orderId, @Param("statuses") Collection<PaymentStatus> statuses);
}
