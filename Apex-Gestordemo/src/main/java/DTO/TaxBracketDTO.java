package DTO;

import java.math.BigDecimal;

public record TaxBracketDTO(
        BigDecimal limite,
        BigDecimal aliquota,
        BigDecimal parcelaDeduzir
) {
}
