package com.nexaerp.vendorbill.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorBillItemResponseDto {
    // Item identifier
    private Long id;

    // Optional product reference (future inventory)
    private Long productId;

    // The expense account this item is posted to
    private Long expenseAccountId;
    private String expenseAccountName;
    private String expenseAccountCode;

    // Optional cost center (future)
    private Long costCenterId;

    // Item details
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    // Discount
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;

    // VAT (Input VAT - we will receive this back)
    private BigDecimal vatRate;
    private BigDecimal vatAmount;

    // TDS (Tax Deducted at Source - we pay to government)
    private BigDecimal tdsRate;
    private BigDecimal tdsAmount;

    // Totals
    private BigDecimal subTotal;   // quantity x unitPrice
    private BigDecimal lineTotal;  // subTotal - discount + VAT
}
