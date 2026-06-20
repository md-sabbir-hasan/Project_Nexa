package com.nexaerp.payment;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.payment.dto.PaymentRequestDto;
import com.nexaerp.payment.dto.PaymentResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> create(
            @Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Payment created",
                paymentService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getAll()));
    }

    @GetMapping("/party/{partyId}")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getByParty(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getByParty(partyId)));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> post(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment posted",
                paymentService.post(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> cancel(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment cancelled",
                paymentService.cancel(id)));
    }
}
