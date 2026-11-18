package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaPagarResponseDTO(Long idContaPagar, Long fornecedorId, String fornecedorNome,
        Long categoriaId, String categoriaNome, String descricao, BigDecimal valor, LocalDate vencimento,
        Long formaPagamentoPrevistaId, String formaPagamentoPrevistaNome, String status,
        Long empresaId, Long filialId) {
}
