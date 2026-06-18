package com.nexaerp.vendorbill;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.vendorbill.dto.VendorBillRequestDto;
import com.nexaerp.vendorbill.dto.VendorBillResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-bills")
@RequiredArgsConstructor
public class VendorBillController {
    private final VendorBillService vendorBillService;

    // Create a new vendor bill in DRAFT status
    @PostMapping
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> create(
            @Valid @RequestBody VendorBillRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill created",
                vendorBillService.create(request)));
    }

    // Update a DRAFT vendor bill
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody VendorBillRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill updated",
                vendorBillService.update(id, request)));
    }

    // Get a single vendor bill by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getById(id)));
    }

    // Get all vendor bills
    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getAll()));
    }

    // Get vendor bills by party (vendor)
    @GetMapping("/party/{partyId}")
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getByParty(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getByParty(partyId)));
    }

    // Get vendor bills filtered by status
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getByStatus(
            @PathVariable VendorBillStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getByStatus(status)));
    }

    // Get vendor bills filtered by bill type
    @GetMapping("/type/{billType}")
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getByBillType(
            @PathVariable VendorBillType billType) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getByBillType(billType)));
    }

    // Approve a DRAFT bill → APPROVED
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> approve(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill approved",
                vendorBillService.approve(id)));
    }

    // Post an APPROVED bill → POSTED (creates Journal Entry)
    @PostMapping("/{id}/post")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> post(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill posted",
                vendorBillService.post(id)));
    }

    // Cancel a vendor bill
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> cancel(
            @PathVariable Long id,
            @RequestParam VendorBillCancelledReason reason) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill cancelled",
                vendorBillService.cancel(id, reason)));
    }
}
