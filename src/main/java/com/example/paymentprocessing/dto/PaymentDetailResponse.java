package com.example.paymentprocessing.dto;

import java.util.List;

public record PaymentDetailResponse(
        PaymentResponse payment,
        List<PaymentStatusHistoryResponse> history
) {
}
