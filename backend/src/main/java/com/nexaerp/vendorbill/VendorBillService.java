package com.nexaerp.vendorbill;

import com.nexaerp.vendorbill.dto.VendorBillRequestDto;
import com.nexaerp.vendorbill.dto.VendorBillResponseDto;

import java.util.List;

public interface VendorBillService {
    // Create a new vendor bill in DRAFT status
    VendorBillResponseDto create(VendorBillRequestDto request);

    // Update a DRAFT vendor bill
    VendorBillResponseDto update(Long id, VendorBillRequestDto request);

    // Get a single vendor bill by ID
    VendorBillResponseDto getById(Long id);

    // Get all vendor bills
    List<VendorBillResponseDto> getAll();

    // Get vendor bills by party (vendor)
    List<VendorBillResponseDto> getByParty(Long partyId);

    // Get vendor bills by status
    List<VendorBillResponseDto> getByStatus(VendorBillStatus status);

    // Get vendor bills by type (EXPENSE, PURCHASE, SERVICE, ASSET)
    List<VendorBillResponseDto> getByBillType(VendorBillType billType);

    // Approve a DRAFT bill → APPROVED
    VendorBillResponseDto approve(Long id);

    // Post an APPROVED bill → POSTED (creates Journal Entry)
    VendorBillResponseDto post(Long id);

    // Cancel a bill (DRAFT or APPROVED or POSTED)
    VendorBillResponseDto cancel(Long id, VendorBillCancelledReason reason);

}
