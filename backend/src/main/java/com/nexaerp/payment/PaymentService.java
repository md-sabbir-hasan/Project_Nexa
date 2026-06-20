package com.nexaerp.payment;

import com.nexaerp.payment.dto.PaymentRequestDto;
import com.nexaerp.payment.dto.PaymentResponseDto;

import java.util.List;

public interface PaymentService {
    // Create a new payment in DRAFT status (with auto or manual allocation saved)
    PaymentResponseDto create(PaymentRequestDto request);

    PaymentResponseDto getById(Long id);

    List<PaymentResponseDto> getAll();

    List<PaymentResponseDto> getByParty(Long partyId);

    // DRAFT → POSTED: creates journal entry, updates invoice/bill paid/due amounts
    PaymentResponseDto post(Long id);

    // Reverses journal entry and undoes invoice/bill due amount changes if already posted
    PaymentResponseDto cancel(Long id);
}
