package com.restaurant.pos.payment;

import com.restaurant.pos.common.BusinessRuleException;
import com.restaurant.pos.common.ResourceNotFoundException;
import com.restaurant.pos.order.Order;
import com.restaurant.pos.order.OrderRepository;
import com.restaurant.pos.order.OrderService;
import com.restaurant.pos.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Set<OrderStatus> PAYABLE = Set.of(OrderStatus.FIRED, OrderStatus.PREPARING, OrderStatus.READY);
    // Counts toward the balance: captured money plus in-flight reservations (§5.1)
    private static final Set<PaymentStatus> HOLDS_BALANCE =
            Set.of(PaymentStatus.PENDING, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * Takes a CASH payment (§8.2). The order row lock serializes payments per order, so the
     * idempotency lookup, balance check and insert below cannot race with another payment.
     */
    @Transactional
    public PaymentResponse pay(UUID orderId, UUID idempotencyKey, CreatePaymentRequest request, UUID userId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey.toString());
        if (existing.isPresent()) {
            return replay(existing.get(), orderId, request);
        }

        if (!PAYABLE.contains(order.getStatus())) {
            throw new BusinessRuleException("Order is not payable in status " + order.getStatus());
        }

        BigDecimal remaining = order.getTotal().subtract(paymentRepository.sumAmount(orderId, HOLDS_BALANCE));
        if (request.amount().compareTo(remaining) > 0) {
            throw new BusinessRuleException("Amount " + request.amount() + " exceeds remaining balance " + remaining);
        }

        BigDecimal change = request.tendered().subtract(request.amount()).subtract(request.tip());
        if (change.signum() < 0) {
            throw new BusinessRuleException("Tendered amount is less than amount + tip");
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setAmount(request.amount());
        payment.setTipAmount(request.tip());
        payment.setTenderedAmount(request.tendered());
        payment.setChangeGiven(change);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now());
        payment.setIdempotencyKey(idempotencyKey.toString());
        payment.setCreatedBy(userId);
        payment = paymentRepository.saveAndFlush(payment);

        BigDecimal captured = paymentRepository.sumAmount(orderId, Set.of(PaymentStatus.CAPTURED));
        if (captured.compareTo(order.getTotal()) == 0) {
            orderService.close(order, OrderStatus.COMPLETED);
        }

        return PaymentResponse.from(payment);
    }

    private PaymentResponse replay(Payment p, UUID orderId, CreatePaymentRequest r) {
        boolean sameRequest = p.getOrderId().equals(orderId)
                && p.getPaymentMethod() == r.method()
                && p.getAmount().compareTo(r.amount()) == 0
                && p.getTipAmount().compareTo(r.tip()) == 0
                && sameMoney(p.getTenderedAmount(), r.tendered());
        if (!sameRequest) {
            throw new BusinessRuleException("Idempotency-Key was already used with a different request");
        }
        return PaymentResponse.from(p);
    }

    private static boolean sameMoney(BigDecimal a, BigDecimal b) {
        return Objects.equals(a, b) || (a != null && b != null && a.compareTo(b) == 0);
    }
}
