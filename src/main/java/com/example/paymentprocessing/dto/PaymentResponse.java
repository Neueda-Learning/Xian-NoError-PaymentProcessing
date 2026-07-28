package com.example.paymentprocessing.dto;

import com.example.paymentprocessing.enums.CurrencyCode;
import com.example.paymentprocessing.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String idempotencyKey,
        String sourceAccount,
        String destinationAccount,
        BigDecimal amount,
        CurrencyCode currency,
        String reference,
        PaymentStatus status,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
