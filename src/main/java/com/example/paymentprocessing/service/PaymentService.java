package com.example.paymentprocessing.service;

import com.example.paymentprocessing.dto.CreatePaymentRequest;
import com.example.paymentprocessing.dto.PaymentResponse;
import com.example.paymentprocessing.entity.Payment;
import com.example.paymentprocessing.entity.PaymentStatusHistory;
import com.example.paymentprocessing.enums.PaymentStatus;
import com.example.paymentprocessing.exception.DuplicatePaymentException;
import com.example.paymentprocessing.exception.PaymentNotFoundException;
import com.example.paymentprocessing.mapper.PaymentMapper;
import com.example.paymentprocessing.repository.AccountRepository;
import com.example.paymentprocessing.repository.PaymentRepository;
import com.example.paymentprocessing.repository.PaymentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PaymentMapper paymentMapper;
    private final AccountRepository accountRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository historyRepository,
            PaymentValidationService validationService,
            PaymentMapper paymentMapper,
            AccountRepository accountRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.validationService = validationService;
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
