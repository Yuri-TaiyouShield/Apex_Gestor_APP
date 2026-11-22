package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VendaDevolucaoResponseDTO(
        Long idVendaDevolucao,
        Long vendaOriginalId,
        String tipo,
        String motivo,
        BigDecimal valorAjuste,
        LocalDateTime criadoEm,
        List<ItemDTO> itens) {
    public record ItemDTO(Long produtoOriginalId, Long produtoNovoId, Integer quantidade, BigDecimal valorUnitario) {
    }
}
