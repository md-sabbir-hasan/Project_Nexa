package com.nexaerp.vendorbill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {
    Optional<VendorBill> findTopByOrderByIdDesc();
    List<VendorBill> findByPartyId(Long partyId);
    List<VendorBill> findByStatus(VendorBillStatus status);
    List<VendorBill> findByBillType(VendorBillType billType);
}
