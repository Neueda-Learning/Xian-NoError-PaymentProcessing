package com.example.paymentprocessing.service;

import com.example.paymentprocessing.entity.Account;
import com.example.paymentprocessing.entity.Payment;
import com.example.paymentprocessing.enums.AccountStatus;
import com.example.paymentprocessing.enums.PaymentErrorCode;
import com.example.paymentprocessing.exception.PaymentValidationException;
import com.example.paymentprocessing.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentValidationService {

    private static final BigDecimal MAX_PAYMENT_AMOUNT =
            new BigDecimal("1000000.00");

    private final AccountRepository accountRepository;

    public PaymentValidationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void validateForProcessing(Payment payment) {
        validateAmount(payment);
        validateAccounts(payment);
    }

    private void validateAmount(Payment payment) {
        if (payment.getAmount() == null) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_AMOUNT,
                    "Payment amount is required"
            );
        }

        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_AMOUNT,
                    "Payment amount must be greater than 0"
            );
        }

        if (payment.getAmount().compareTo(MAX_PAYMENT_AMOUNT) > 0) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_AMOUNT,
                    "Payment amount must not exceed 1000000.00"
            );
        }
    }

    private void validateAccounts(Payment payment) {
        if (payment.getSourceAccount() == null
                || payment.getSourceAccount().isBlank()) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_ACCOUNT,
                    "Source account is required"
            );
        }

        if (payment.getDestinationAccount() == null
                || payment.getDestinationAccount().isBlank()) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_ACCOUNT,
                    "Destination account is required"
            );
        }

        if (payment.getSourceAccount().equals(payment.getDestinationAccount())) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_ACCOUNT,
                    "Source account and destination account must be different"
            );
        }

        Account sourceAccount = accountRepository
                .findByAccountNumber(payment.getSourceAccount())
                .orElseThrow(() -> new PaymentValidationException(
                        PaymentErrorCode.INVALID_ACCOUNT,
                        "Source account does not exist"
                ));

        Account destinationAccount = accountRepository
                .findByAccountNumber(payment.getDestinationAccount())
                .orElseThrow(() -> new PaymentValidationException(
                        PaymentErrorCode.INVALID_ACCOUNT,
                        "Destination account does not exist"
                ));

        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_ACCOUNT,
                    "Source account is not active"
            );
        }

        if (destinationAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_ACCOUNT,
                    "Destination account is not active"
            );
        }

        if (sourceAccount.getCurrency() != payment.getCurrency()) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_CURRENCY,
                    "Payment currency does not match source account currency"
            );
        }

        if (destinationAccount.getCurrency() != payment.getCurrency()) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_CURRENCY,
                    "Payment currency does not match destination account currency"
            );
        }

        if (sourceAccount.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INSUFFICIENT_FUNDS,
                    "Source account has insufficient funds"
            );
        }
    }
}