package com.restaurant.pos.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        BigDecimal tip,
        BigDecimal tendered,
        BigDecimal changeGiven,
        Instant capturedAt,
        Instant createdAt
) {
    static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getPaymentMethod(), p.getStatus(),
                p.getAmount(), p.getTipAmount(), p.getTenderedAmount(), p.getChangeGiven(),
                p.getCapturedAt(), p.getCreatedAt());
    }
}
