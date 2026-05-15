package DTO;

import java.math.BigDecimal;

public record PricingCalculationResponseDTO(
        String tenantCode,
        String planCode,
        BigDecimal productCost,
        BigDecimal fixedExpensePercent,
        BigDecimal taxPercent,
        BigDecimal desiredMarginPercent,
        BigDecimal sellerCommissionPercent,
        BigDecimal suggestedPrice,
        BigDecimal expectedProfit,
        Long commissionPoolId,
        String message
) {
}
