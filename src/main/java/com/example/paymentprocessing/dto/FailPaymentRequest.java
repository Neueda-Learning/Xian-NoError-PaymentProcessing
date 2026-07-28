package com.example.paymentprocessing.dto;

import com.example.paymentprocessing.enums.PaymentErrorCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FailPaymentRequest(
        @NotNull(message = "Error code is required")
        PaymentErrorCode errorCode,

        @Size(max = 500, message = "Error message must be at most 500 characters")
        String errorMessage
) {
}
