package com.example.paymentprocessing.repository;

import com.example.paymentprocessing.entity.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {

    List<PaymentStatusHistory> findAllByPaymentIdOrderByChangedAtAsc(Long paymentId);
}
