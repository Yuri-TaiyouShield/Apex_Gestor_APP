package DTO;

import java.math.BigDecimal;

public record B2cCartMergeResponseDTO(
        Long cartId,
        Long clienteId,
        int totalItems,
        BigDecimal subtotal,
        String status
) {
}
