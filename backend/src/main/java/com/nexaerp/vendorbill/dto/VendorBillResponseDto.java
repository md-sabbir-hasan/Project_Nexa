package com.nexaerp.vendorbill.dto;

import com.nexaerp.vendorbill.VendorBillCancelledReason;
import com.nexaerp.vendorbill.VendorBillReferenceType;
import com.nexaerp.vendorbill.VendorBillStatus;
import com.nexaerp.vendorbill.VendorBillType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorBillResponseDto {
    // Bill identifier
    private Long id;

    // Auto-generated bill number (e.g. BILL-2025-000001)
    private String billNumber;

    // Dates
    private LocalDate billDate;       // vendor's invoice date
    private LocalDate postingDate;    // accounting date
    private LocalDate dueDate;        // payment deadline

    // Vendor's own invoice number
    private String vendorBillRef;

    // Vendor details
    private Long partyId;
    private String partyName;

    // Bill classification
    private VendorBillType billType;
    private VendorBillStatus status;

    // Currency
    private String currencyCode;
    private BigDecimal exchangeRate;
    private Integer paymentTerms;

    // Reference to source document
    private VendorBillReferenceType referenceType;
    private String referenceId;

    // Additional info
    private String notes;
    private VendorBillCancelledReason cancelledReason;

    // Calculated totals (stored in DB for performance)
    private BigDecimal subTotal;        // sum of all item subtotals
    private BigDecimal discountAmount;  // sum of all discounts
    private BigDecimal vatAmount;       // total Input VAT
    private BigDecimal tdsAmount;       // total TDS to pay government
    private BigDecimal grandTotal;      // subTotal - discount + VAT
    private BigDecimal netPayable;      // grandTotal - TDS
    private BigDecimal paidAmount;      // how much has been paid
    private BigDecimal dueAmount;       // netPayable - paidAmount

    // Workflow timestamps
    private LocalDateTime approvedAt;
    private LocalDateTime postedAt;

    // Audit timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Bill line items
    private List<VendorBillItemResponseDto> items;
}
