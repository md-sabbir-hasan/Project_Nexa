package com.nexaerp.payment;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.journal.*;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.payment.dto.PaymentAllocationRequestDto;
import com.nexaerp.payment.dto.PaymentAllocationResponseDto;
import com.nexaerp.payment.dto.PaymentRequestDto;
import com.nexaerp.payment.dto.PaymentResponseDto;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PartyRepository partyRepository;
    private final AccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;




    @Override
    @Transactional
    public PaymentResponseDto create(PaymentRequestDto request) {
        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Build payment header
        Payment payment = new Payment();
        payment.setPaymentNumber(generatePaymentNumber());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentType(request.getPaymentType());
        payment.setParty(party);
        payment.setAccount(account);
        payment.setAmount(request.getAmount());
        payment.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "BDT");
        payment.setExchangeRate(BigDecimal.ONE);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionRef(request.getTransactionRef());
        payment.setNotes(request.getNotes());
        payment.setStatus(PaymentStatus.DRAFT);

        Payment savedPayment = paymentRepository.save(payment);

        // Build allocation list — either auto (FIFO) or manual (from request)
        List<PaymentAllocation> allocations;

        if (Boolean.TRUE.equals(request.getAutoAllocate())) {
            allocations = autoAllocateFifo(savedPayment, party.getId());
        } else {
            allocations = buildManualAllocations(request.getAllocations(), savedPayment);
        }

        paymentAllocationRepository.saveAll(allocations);

        // Calculate allocatedAmount and unallocatedAmount and store in payment
        BigDecimal totalAllocated = allocations.stream()
                .map(PaymentAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        savedPayment.setAllocatedAmount(totalAllocated);
        savedPayment.setUnallocatedAmount(savedPayment.getAmount().subtract(totalAllocated));
        paymentRepository.save(savedPayment);

        return toResponse(savedPayment);
    }

    @Override
    public PaymentResponseDto getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment);
    }

    @Override
    public List<PaymentResponseDto> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponseDto> getByParty(Long partyId) {
        return paymentRepository.findByPartyId(partyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponseDto post(Long id) {
        return null;
    }

    @Override
    public PaymentResponseDto cancel(Long id) {
        return null;
    }






                    //    -----Allocation Helper Method-------



//      FIFO auto allocation: picks the oldest unpaid invoices/bills first
//      and fills them one by one until the payment amount runs out.

    private List<PaymentAllocation> autoAllocateFifo(Payment payment, Long partyId) {

        BigDecimal remaining = payment.getAmount();
        List<PaymentAllocation> allocations = new java.util.ArrayList<>();

        if (payment.getPaymentType() == PaymentType.RECEIPT) {

            // Customer payment → allocate against Invoices
            List<Invoice> dueInvoices = invoiceRepository
                    .findByPartyIdAndDueAmountGreaterThanOrderByDueDateAsc(partyId, BigDecimal.ZERO);

            for (Invoice invoice : dueInvoices) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal allocateAmount = remaining.min(invoice.getDueAmount());

                allocations.add(PaymentAllocation.builder()
                        .payment(payment)
                        .referenceType(PaymentReferenceType.INVOICE)
                        .referenceId(invoice.getId())
                        .allocatedAmount(allocateAmount)
                        .build());

                remaining = remaining.subtract(allocateAmount);
            }

        } else {

            // Vendor payment → allocate against Vendor Bills
            List<VendorBill> dueBills = vendorBillRepository
                    .findByPartyIdAndDueAmountGreaterThanOrderByDueDateAsc(partyId, BigDecimal.ZERO);

            for (VendorBill bill : dueBills) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal allocateAmount = remaining.min(bill.getDueAmount());

                allocations.add(PaymentAllocation.builder()
                        .payment(payment)
                        .referenceType(PaymentReferenceType.VENDOR_BILL)
                        .referenceId(bill.getId())
                        .allocatedAmount(allocateAmount)
                        .build());

                remaining = remaining.subtract(allocateAmount);
            }
        }
        // Whatever is left after all due documents are cleared stays unallocated (advance)
        return allocations;
    }



//      Manual allocation: uses exactly what the user specified in the request.
//      Validates that total allocated does not exceed the payment amount.

    private List<PaymentAllocation> buildManualAllocations(
            List<PaymentAllocationRequestDto> requestAllocations, Payment payment) {

        if (requestAllocations == null || requestAllocations.isEmpty()) {
            // No allocation provided — entire amount stays as advance
            return List.of();
        }

        BigDecimal totalRequested = requestAllocations.stream()
                .map(PaymentAllocationRequestDto::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRequested.compareTo(payment.getAmount()) > 0) {
            throw new BusinessRuleException(
                    "Total allocated amount cannot exceed payment amount");
        }

        return requestAllocations.stream()
                .map(dto -> PaymentAllocation.builder()
                        .payment(payment)
                        .referenceType(dto.getReferenceType())
                        .referenceId(dto.getReferenceId())
                        .allocatedAmount(dto.getAllocatedAmount())
                        .build())
                .collect(Collectors.toList());
    }


//      Applies one allocation's amount to the actual Invoice or VendorBill —
//      updates paidAmount, dueAmount, and status. Called only during POST.

    private void applyAllocationToDocument(PaymentAllocation allocation) {

        if (allocation.getReferenceType() == PaymentReferenceType.INVOICE) {

            Invoice invoice = invoiceRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

            invoice.setPaidAmount(invoice.getPaidAmount().add(allocation.getAllocatedAmount()));
            invoice.setDueAmount(invoice.getGrandTotal().subtract(invoice.getPaidAmount()));

            invoice.setStatus(invoice.getDueAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? InvoiceStatus.PAID
                    : InvoiceStatus.PARTIAL);

            invoiceRepository.save(invoice);

        } else {

            VendorBill bill = vendorBillRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

            bill.setPaidAmount(bill.getPaidAmount().add(allocation.getAllocatedAmount()));
            bill.setDueAmount(bill.getNetPayable().subtract(bill.getPaidAmount()));

            bill.setStatus(bill.getDueAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? VendorBillStatus.PAID
                    : VendorBillStatus.PARTIAL);

            vendorBillRepository.save(bill);
        }
    }


//     Undoes what applyAllocationToDocument did — used when a posted payment is canceled.

    private void undoAllocationFromDocument(PaymentAllocation allocation) {

        if (allocation.getReferenceType() == PaymentReferenceType.INVOICE) {

            Invoice invoice = invoiceRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

            invoice.setPaidAmount(invoice.getPaidAmount().subtract(allocation.getAllocatedAmount()));
            invoice.setDueAmount(invoice.getGrandTotal().subtract(invoice.getPaidAmount()));

            invoice.setStatus(invoice.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? InvoiceStatus.POSTED
                    : InvoiceStatus.PARTIAL);

            invoiceRepository.save(invoice);

        } else {

            VendorBill bill = vendorBillRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

            bill.setPaidAmount(bill.getPaidAmount().subtract(allocation.getAllocatedAmount()));
            bill.setDueAmount(bill.getNetPayable().subtract(bill.getPaidAmount()));

            bill.setStatus(bill.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? VendorBillStatus.POSTED
                    : VendorBillStatus.PARTIAL);

            vendorBillRepository.save(bill);
        }
    }


                                   // ----Journal Entry Helper------


    private void createJournalEntry(Payment payment) {

        Account receivable = accountRepository.findByCode("1120")
                .orElseThrow(() -> new ResourceNotFoundException("Accounts Receivable not found"));
        Account payable = accountRepository.findByCode("2110")
                .orElseThrow(() -> new ResourceNotFoundException("Accounts Payable not found"));

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(payment.getPaymentDate());
        entry.setDescription("Payment - " + payment.getPaymentNumber());
        entry.setType(JournalEntryType.CASH);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.PAYMENT);
        entry.setSourceId(payment.getId());
        entry.setTotalAmount(payment.getAmount());

        JournalEntry saved = journalEntryRepository.save(entry);

        if (payment.getPaymentType() == PaymentType.RECEIPT) {
            // Money coming in: Debit Cash/Bank, Credit Accounts Receivable
            saveLineAndUpdateBalance(saved, payment.getAccount(), payment.getAmount(), BigDecimal.ZERO);
            saveLineAndUpdateBalance(saved, receivable, BigDecimal.ZERO, payment.getAmount());
        } else {
            // Money going out: Debit Accounts Payable, Credit Cash/Bank
            saveLineAndUpdateBalance(saved, payable, payment.getAmount(), BigDecimal.ZERO);
            saveLineAndUpdateBalance(saved, payment.getAccount(), BigDecimal.ZERO, payment.getAmount());
        }
    }

    private void reverseJournalEntry(Payment payment) {

        journalEntryRepository
                .findBySourceTypeAndSourceId(JournalSourceType.PAYMENT, payment.getId())
                .ifPresent(original -> {

                    JournalEntry reversal = new JournalEntry();
                    reversal.setEntryNumber(generateJournalNumber());
                    reversal.setDate(LocalDate.now());
                    reversal.setDescription("Reversal - " + payment.getPaymentNumber());
                    reversal.setType(JournalEntryType.CASH);
                    reversal.setStatus(JournalStatus.POSTED);
                    reversal.setSourceType(JournalSourceType.PAYMENT);
                    reversal.setSourceId(payment.getId());
                    reversal.setTotalAmount(original.getTotalAmount());
                    reversal.setReversedFromId(original.getId());

                    JournalEntry savedReversal = journalEntryRepository.save(reversal);

                    List<JournalLine> originalLines =
                            journalLineRepository.findByJournalEntryId(original.getId());

                    originalLines.forEach(line -> {
                        saveLineAndUpdateBalance(savedReversal, line.getAccount(),
                                line.getCredit(), line.getDebit()); // swapped
                    });

                    original.setStatus(JournalStatus.REVERSED);
                    journalEntryRepository.save(original);
                });
    }

    private void saveLineAndUpdateBalance(JournalEntry entry, Account account,
                                          BigDecimal debit, BigDecimal credit) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDebit(debit);
        line.setCredit(credit);
        journalLineRepository.save(line);

        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(account.getCurrentBalance().add(debit).subtract(credit));
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(account.getCurrentBalance().add(credit).subtract(debit));
                break;
        }
        accountRepository.save(account);
    }



                                  // -------Number Generators--------

    private String generatePaymentNumber() {
        int year = Year.now().getValue();
        return paymentRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String[] parts = last.getPaymentNumber().split("-");
                    int next = Integer.parseInt(parts[2]) + 1;
                    return String.format("PAY-%d-%06d", year, next);
                })
                .orElse(String.format("PAY-%d-%06d", year, 1));
    }

    private String generateJournalNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getEntryNumber().replace("JE-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("JE-%04d", next);
                })
                .orElse("JE-0001");
    }


                                     // -------Mapper---------


    private PaymentResponseDto toResponse(Payment payment) {
        List<PaymentAllocation> allocations =
                paymentAllocationRepository.findByPaymentId(payment.getId());

        return PaymentResponseDto.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .paymentDate(payment.getPaymentDate())
                .paymentType(payment.getPaymentType())
                .partyId(payment.getParty().getId())
                .partyName(payment.getParty().getName())
                .accountId(payment.getAccount().getId())
                .accountName(payment.getAccount().getName())
                .amount(payment.getAmount())
                .allocatedAmount(payment.getAllocatedAmount())
                .unallocatedAmount(payment.getUnallocatedAmount())
                .currencyCode(payment.getCurrencyCode())
                .exchangeRate(payment.getExchangeRate())
                .paymentMethod(payment.getPaymentMethod())
                .transactionRef(payment.getTransactionRef())
                .notes(payment.getNotes())
                .status(payment.getStatus())
                .postedAt(payment.getPostedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .allocations(allocations.stream()
                        .map(this::toAllocationResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private PaymentAllocationResponseDto toAllocationResponse(PaymentAllocation allocation) {
        return PaymentAllocationResponseDto.builder()
                .id(allocation.getId())
                .referenceType(allocation.getReferenceType())
                .referenceId(allocation.getReferenceId())
                .allocatedAmount(allocation.getAllocatedAmount())
                .createdAt(allocation.getCreatedAt())
                .build();
    }
}
