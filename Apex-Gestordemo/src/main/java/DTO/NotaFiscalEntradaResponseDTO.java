package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record NotaFiscalEntradaResponseDTO(Long idNfEntrada, String numero, String serie,
        Long fornecedorId, String fornecedorNome, LocalDateTime dataEmissao,
        LocalDateTime dataEntrada, BigDecimal valorTotal, Long empresaId, Long filialId,
        List<ItemDTO> itens) {
    public record ItemDTO(Long idItemNf, Long produtoId, String produtoDescricao,
            Integer quantidade, BigDecimal valorCustoUnitario) {
    }
}
