package com.example.paymentprocessing.dto;

import com.example.paymentprocessing.enums.CurrencyCode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank(message = "Idempotency key is required")
        @Size(max = 100, message = "Idempotency key must be at most 100 characters")
        String idempotencyKey,

        @NotBlank(message = "Source account is required")
        @Size(max = 50, message = "Source account must be at most 50 characters")
        String sourceAccount,

        @NotBlank(message = "Destination account is required")
        @Size(max = 50, message = "Destination account must be at most 50 characters")
        String destinationAccount,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Amount can have maximum 13 integer digits and 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        CurrencyCode currency,

        @Size(max = 255, message = "Reference must be at most 255 characters")
        String reference
) {
}
