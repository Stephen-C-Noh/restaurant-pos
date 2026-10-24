package com.restaurant.pos.payment;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull PaymentMethod method,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal tip,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal tendered
) {
    public CreatePaymentRequest {
        if (tip == null) tip = BigDecimal.ZERO;
    }

    // ponytail: CASH only until card payments land (issue #11)
    @AssertTrue(message = "only CASH is supported")
    public boolean isMethodSupported() {
        return method == null || method == PaymentMethod.CASH;
    }

    @AssertTrue(message = "tendered is required for CASH")
    public boolean isTenderedPresentForCash() {
        return method != PaymentMethod.CASH || tendered != null;
    }
}
