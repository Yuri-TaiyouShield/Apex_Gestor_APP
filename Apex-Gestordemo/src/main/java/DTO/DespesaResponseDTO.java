package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaResponseDTO(
        Long idDespesa,
        String descricao,
        BigDecimal valor,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        LocalDate dataCompetencia,
        LocalDate dataEmissao,
        Integer status,
        Long empresaId,
        Long filialId,
        Long tipoDespesaId,
        String tipoDespesaNome,
        TipoDespesaResponseDTO tipoDespesa,
        Long planoContaId,
        String planoContaCodigo,
        String planoContaNome,
        Long centroCustoId,
        String centroCustoCodigo,
        String centroCustoNome) {
}
