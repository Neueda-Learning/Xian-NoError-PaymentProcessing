package com.example.paymentprocessing.service;

import com.example.paymentprocessing.dto.*;
import com.example.paymentprocessing.entity.Account;
import com.example.paymentprocessing.entity.Payment;
import com.example.paymentprocessing.entity.PaymentStatusHistory;
import com.example.paymentprocessing.enums.PaymentErrorCode;
import com.example.paymentprocessing.enums.PaymentStatus;
import com.example.paymentprocessing.exception.DuplicatePaymentException;
import com.example.paymentprocessing.exception.InvalidStatusTransitionException;
import com.example.paymentprocessing.exception.PaymentNotFoundException;
import com.example.paymentprocessing.exception.PaymentValidationException;
import com.example.paymentprocessing.mapper.PaymentMapper;
import com.example.paymentprocessing.repository.AccountRepository;
import com.example.paymentprocessing.repository.PaymentRepository;
import com.example.paymentprocessing.repository.PaymentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentService {

    private static final String SYSTEM_USER = "SYSTEM";

    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, Set.of(PaymentStatus.VALIDATED, PaymentStatus.FAILED),
            PaymentStatus.VALIDATED, Set.of(PaymentStatus.SENT, PaymentStatus.FAILED),
            PaymentStatus.SENT, Set.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED),
            PaymentStatus.COMPLETED, Set.of(),
            PaymentStatus.FAILED, Set.of()
    );

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final PaymentValidationService validationService;
        private final CurrencyConversionService currencyConversionService;
    private final PaymentMapper paymentMapper;
    private final AccountRepository accountRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository historyRepository,
            PaymentValidationService validationService,
                        CurrencyConversionService currencyConversionService,
            PaymentMapper paymentMapper,
            AccountRepository accountRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.validationService = validationService;
                this.currencyConversionService = currencyConversionService;
        this.paymentMapper = paymentMapper;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (paymentRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            throw new DuplicatePaymentException(request.idempotencyKey());
        }

        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment();
        payment.setIdempotencyKey(request.idempotencyKey());
        payment.setSourceAccount(request.sourceAccount());
        payment.setDestinationAccount(request.destinationAccount());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setReference(request.reference());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        Payment savedPayment = paymentRepository.save(payment);

        saveHistory(
                savedPayment.getId(),
                null,
                PaymentStatus.CREATED,
                "Payment created",
                SYSTEM_USER
        );

        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(PaymentStatus status) {
        List<Payment> payments = status == null
                ? paymentRepository.findAllByOrderByCreatedAtDesc()
                : paymentRepository.findAllByStatusOrderByCreatedAtDesc(status);

        return payments.stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        return paymentMapper.toResponse(getPaymentOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetails(Long id) {
        Payment payment = getPaymentOrThrow(id);

        List<PaymentStatusHistoryResponse> history = historyRepository
                .findAllByPaymentIdOrderByChangedAtAsc(id)
                .stream()
                .map(paymentMapper::toHistoryResponse)
                .toList();

        return new PaymentDetailResponse(
                paymentMapper.toResponse(payment),
                history
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentStatusHistoryResponse> getPaymentHistory(Long id) {
        getPaymentOrThrow(id);

        return historyRepository
                .findAllByPaymentIdOrderByChangedAtAsc(id)
                .stream()
                .map(paymentMapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse validatePayment(Long id) {
        Payment payment = getPaymentOrThrow(id);

        try {
            validationService.validateForProcessing(payment);

            return transitionPayment(
                    payment,
                    PaymentStatus.VALIDATED,
                    "Payment passed validation",
                    null,
                    null
            );

        } catch (PaymentValidationException exception) {
            return transitionPayment(
                    payment,
                    PaymentStatus.FAILED,
                    "Validation failed: " + exception.getMessage(),
                    exception.getErrorCode().name(),
                    exception.getMessage()
            );
        }
    }

    @Transactional
    public PaymentResponse sendPayment(Long id) {
        Payment payment = getPaymentOrThrow(id);

        if (payment.getReference() != null
                && payment.getReference().toUpperCase().contains("FAIL-SEND")) {
            return transitionPayment(
                    payment,
                    PaymentStatus.FAILED,
                    "Simulated network failure during sending",
                    PaymentErrorCode.NETWORK_ERROR.name(),
                    "Simulated payment network unavailable"
            );
        }

        return transitionPayment(
                payment,
                PaymentStatus.SENT,
                "Payment sent to simulated payment network",
                null,
                null
        );
    }

    @Transactional
    public PaymentResponse completePayment(Long id) {
        Payment payment = getPaymentOrThrow(id);

        validateTransition(payment.getStatus(), PaymentStatus.COMPLETED);

        try {
            validationService.validateForProcessing(payment);

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

            sourceAccount.debit(payment.getAmount());

            BigDecimal creditedAmount = currencyConversionService.convert(
                    payment.getAmount(),
                    payment.getCurrency(),
                    destinationAccount.getCurrency()
            );

            destinationAccount.credit(creditedAmount);

            accountRepository.save(sourceAccount);
            accountRepository.save(destinationAccount);

            return transitionPayment(
                    payment,
                    PaymentStatus.COMPLETED,
                    buildCompletionReason(payment, destinationAccount, creditedAmount),
                    null,
                    null
            );

        } catch (PaymentValidationException exception) {
            return transitionPayment(
                    payment,
                    PaymentStatus.FAILED,
                    "Payment completion failed: " + exception.getMessage(),
                    exception.getErrorCode().name(),
                    exception.getMessage()
            );
        }
    }

    @Transactional
    public PaymentResponse failPayment(Long id, FailPaymentRequest request) {
        Payment payment = getPaymentOrThrow(id);

        String message = request.errorMessage() == null || request.errorMessage().isBlank()
                ? "Payment manually failed"
                : request.errorMessage();

        return transitionPayment(
                payment,
                PaymentStatus.FAILED,
                "Payment failed manually: " + message,
                request.errorCode().name(),
                message
        );
    }

    private PaymentResponse transitionPayment(
            Payment payment,
            PaymentStatus newStatus,
            String reason,
            String errorCode,
            String errorMessage
    ) {
        PaymentStatus previousStatus = payment.getStatus();
        validateTransition(previousStatus, newStatus);

        payment.setStatus(newStatus);
        payment.setUpdatedAt(LocalDateTime.now());

        if (newStatus == PaymentStatus.FAILED) {
            payment.setErrorCode(errorCode);
            payment.setErrorMessage(errorMessage);
        } else {
            payment.setErrorCode(null);
            payment.setErrorMessage(null);
        }

        Payment savedPayment = paymentRepository.save(payment);

        saveHistory(
                savedPayment.getId(),
                previousStatus,
                newStatus,
                reason,
                SYSTEM_USER
        );

        return paymentMapper.toResponse(savedPayment);
    }

        private String buildCompletionReason(
                        Payment payment,
                        Account destinationAccount,
                        BigDecimal creditedAmount
        ) {
                if (payment.getCurrency() == destinationAccount.getCurrency()) {
                        return "Payment successfully completed and account balances updated";
                }

                return String.format(
                                "Payment successfully completed and account balances updated with currency conversion: %s %s -> %s %s",
                                payment.getAmount(),
                                payment.getCurrency(),
                                creditedAmount,
                                destinationAccount.getCurrency()
                );
        }

    private void validateTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        if (!VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }

    private Payment getPaymentOrThrow(Long id) {
        return paymentRepository
                .findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private void saveHistory(
            Long paymentId,
            PaymentStatus previousStatus,
            PaymentStatus newStatus,
            String reason,
            String triggeredBy
    ) {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPaymentId(paymentId);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setTriggeredBy(triggeredBy);
        history.setChangedAt(LocalDateTime.now());

        historyRepository.save(history);
    }
}