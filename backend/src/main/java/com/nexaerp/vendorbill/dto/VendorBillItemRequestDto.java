package com.nexaerp.vendorbill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorBillItemRequestDto {
    // Optional - will be used when inventory module is added
    private Long productId;

    // Required - which account this expense belongs to (Rent, Internet, Salary)
    @NotNull(message = "Expense account is required")
    private Long expenseAccountId;

    // Optional - for future department/cost center reporting
    private Long costCenterId;

    // Required - what was purchased or what service was received
    @NotBlank(message = "Description is required")
    private String description;

    // Required - how many units
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    // Required - price per unit
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0")
    private BigDecimal unitPrice;

    // Optional - discount percentage on this item (default 0)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    // Optional - VAT percentage on this item (default 0)
    private BigDecimal vatRate = BigDecimal.ZERO;

    // Optional - TDS percentage on this item (default 0)
    private BigDecimal tdsRate = BigDecimal.ZERO;
}
