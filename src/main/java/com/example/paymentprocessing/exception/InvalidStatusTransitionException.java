package com.example.paymentprocessing.exception;

import com.example.paymentprocessing.enums.PaymentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(PaymentStatus currentStatus, PaymentStatus newStatus) {
        super("Cannot transition payment from " + currentStatus + " to " + newStatus);
    }
}
