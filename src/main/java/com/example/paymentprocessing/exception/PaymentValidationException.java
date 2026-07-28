package com.example.paymentprocessing.exception;

import com.example.paymentprocessing.enums.PaymentErrorCode;

public class PaymentValidationException extends RuntimeException {

    private final PaymentErrorCode errorCode;

    public PaymentValidationException(
            PaymentErrorCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentErrorCode getErrorCode() {
        return errorCode;
    }
}