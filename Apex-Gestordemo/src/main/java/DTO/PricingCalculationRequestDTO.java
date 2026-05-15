package DTO;

import java.math.BigDecimal;

public record PricingCalculationRequestDTO(
        BigDecimal productCost,
        BigDecimal fixedExpensePercent,
        BigDecimal taxPercent,
        BigDecimal desiredMarginPercent,
        BigDecimal sellerCommissionPercent,
        boolean onlineSale,
        String branchCode,
        String orderId,
        BigDecimal grossAmount
) {
}
