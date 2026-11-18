package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FluxoCaixaPrevistoItemDTO(
        String origemTipo,
        Long registroId,
        String natureza,
        String descricao,
        String parteRelacionada,
        LocalDate dataCompetencia,
        LocalDate dataVencimento,
        LocalDate dataPrevistaLiquidacao,
        int numeroParcela,
        int totalParcelas,
        String modalidade,
        String situacaoVencimento,
        long diasAtrasoOuPrazo,
        BigDecimal valorBruto,
        BigDecimal valorPagoAcumulado,
        BigDecimal valorTaxaPrevista,
        BigDecimal saldoAbertoLiquido,
        boolean recorrente,
        boolean antecipado,
        Long planoContaId,
        String planoContaNome,
        Long centroCustoId,
        String centroCustoNome,
        Long filialId) {
}
