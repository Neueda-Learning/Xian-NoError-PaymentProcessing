package com.example.paymentprocessing.controller;

import com.example.paymentprocessing.dto.FailPaymentRequest;
import com.example.paymentprocessing.dto.PaymentDetailResponse;
import com.example.paymentprocessing.dto.PaymentResponse;
import com.example.paymentprocessing.dto.PaymentStatusHistoryResponse;
import com.example.paymentprocessing.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
