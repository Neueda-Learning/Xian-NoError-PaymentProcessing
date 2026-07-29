package com.example.paymentprocessing.controller;

import com.example.paymentprocessing.dto.PaymentDetailResponse;
import com.example.paymentprocessing.dto.PaymentStatusHistoryResponse;
import com.example.paymentprocessing.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
