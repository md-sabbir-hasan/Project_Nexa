package com.nexaerp.invoice;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.invoice.dto.InvoiceRequestDto;
import com.nexaerp.invoice.dto.InvoiceResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> create(
            @Valid @RequestBody InvoiceRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Invoice created",
                invoiceService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Invoice updated",
                invoiceService.update(id, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAll()));
    }

    @GetMapping("/party/{partyId}")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getByParty(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByParty(partyId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getByStatus(
            @PathVariable InvoiceStatus status) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByStatus(status)));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> post(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice posted",
                invoiceService.post(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> cancel(
            @PathVariable Long id,
            @RequestParam CancelledReason reason) {
        return ResponseEntity.ok(ApiResponse.success("Invoice cancelled",
                invoiceService.cancel(id, reason)));
    }
}
