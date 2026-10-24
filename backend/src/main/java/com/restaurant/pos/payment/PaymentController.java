package com.restaurant.pos.payment;

import com.restaurant.pos.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // A replay returns the stored payment with the same 201 as the original call
    @PostMapping
    @PreAuthorize("hasAnyRole('SERVER','MANAGER','ADMIN')")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal User user) {
        PaymentResponse payment = paymentService.pay(orderId, idempotencyKey, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }
}
