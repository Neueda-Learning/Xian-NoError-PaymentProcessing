package com.example.paymentprocessing.dto;

import com.example.paymentprocessing.enums.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentStatusHistoryResponse(
        Long id,
        Long paymentId,
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        String reason,
        String triggeredBy,
        LocalDateTime changedAt
) {
}
