package com.example.paymentprocessing.entity;

import com.example.paymentprocessing.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_status_history")
public class PaymentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private PaymentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private PaymentStatus newStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(PaymentStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public PaymentStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PaymentStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
