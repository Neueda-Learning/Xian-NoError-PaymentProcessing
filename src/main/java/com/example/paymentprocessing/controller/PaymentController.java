package com.example.paymentprocessing.controller;

import com.example.paymentprocessing.dto.*;
import com.example.paymentprocessing.enums.PaymentStatus;
import com.example.paymentprocessing.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<PaymentResponse> getPayments(
            @RequestParam(required = false) PaymentStatus status
    ) {
        return paymentService.getPayments(status);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable Long id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/{id}/details")
    public PaymentDetailResponse getPaymentDetails(@PathVariable Long id) {
        return paymentService.getPaymentDetails(id);
    }

    @GetMapping("/{id}/history")
    public List<PaymentStatusHistoryResponse> getPaymentHistory(@PathVariable Long id) {
        return paymentService.getPaymentHistory(id);
    }

    @PostMapping("/{id}/validate")
    public PaymentResponse validatePayment(@PathVariable Long id) {
        return paymentService.validatePayment(id);
    }

    @PostMapping("/{id}/send")
    public PaymentResponse sendPayment(@PathVariable Long id) {
        return paymentService.sendPayment(id);
    }

    @PostMapping("/{id}/complete")
    public PaymentResponse completePayment(@PathVariable Long id) {
        return paymentService.completePayment(id);
    }

    @PostMapping("/{id}/fail")
    public PaymentResponse failPayment(
            @PathVariable Long id,
            @Valid @RequestBody FailPaymentRequest request
    ) {
        return paymentService.failPayment(id, request);
    }
}