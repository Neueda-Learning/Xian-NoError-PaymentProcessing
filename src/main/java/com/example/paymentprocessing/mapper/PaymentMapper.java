package com.example.paymentprocessing.mapper;

import com.example.paymentprocessing.dto.PaymentResponse;
import com.example.paymentprocessing.dto.PaymentStatusHistoryResponse;
import com.example.paymentprocessing.entity.Payment;
import com.example.paymentprocessing.entity.PaymentStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getSourceAccount(),
                payment.getDestinationAccount(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus(),
                payment.getErrorCode(),
                payment.getErrorMessage(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public PaymentStatusHistoryResponse toHistoryResponse(PaymentStatusHistory history) {
        return new PaymentStatusHistoryResponse(
                history.getId(),
                history.getPaymentId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getReason(),
                history.getTriggeredBy(),
                history.getChangedAt()
        );
    }
}
