package com.nexaerp.vendorbill.dto;

import com.nexaerp.vendorbill.VendorBillReferenceType;
import com.nexaerp.vendorbill.VendorBillType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorBillRequestDto {
    // Required - which vendor sent this bill
    @NotNull(message = "Party is required")
    private Long partyId;

    // Required - the date on the vendor's invoice
    @NotNull(message = "Bill date is required")
    private LocalDate billDate;

    // Optional - the date this bill will be recorded in accounting
    // If not provided, billDate will be used
    private LocalDate postingDate;

    // Optional - vendor's own invoice number for reference
    private String vendorBillRef;

    // Required - type of bill (EXPENSE, PURCHASE, SERVICE, ASSET)
    @NotNull(message = "Bill type is required")
    private VendorBillType billType;

    // Optional - payment terms in days (default 30)
    private Integer paymentTerms;

    // Optional - currency (default BDT)
    private String currencyCode;

    // Optional - how this bill was created (PURCHASE_ORDER, GOODS_RECEIPT, MANUAL)
    private VendorBillReferenceType referenceType;

    // Optional - reference document number (e.g. PO-0001)
    private String referenceId;

    // Optional - any additional notes
    private String notes;

    // Required - at least one item must be provided
    @NotNull(message = "Items are required")
    @Size(min = 1, message = "At least 1 item required")
    private List<VendorBillItemRequestDto> items;
}
