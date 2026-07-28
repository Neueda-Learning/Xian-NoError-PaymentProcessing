package com.example.paymentprocessing.repository;

import com.example.paymentprocessing.entity.Payment;
import com.example.paymentprocessing.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Payment> findAllByStatusOrderByCreatedAtDesc(PaymentStatus status);

    List<Payment> findAllByOrderByCreatedAtDesc();
}
