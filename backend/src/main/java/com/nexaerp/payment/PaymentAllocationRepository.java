package com.nexaerp.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findByPaymentId(Long paymentId);

    // Used to find all allocations made against a specific invoice/bill
    // (needed when calculating how much of that invoice is already paid)
    List<PaymentAllocation> findByReferenceTypeAndReferenceId(
            PaymentReferenceType referenceType, Long referenceId);
}
