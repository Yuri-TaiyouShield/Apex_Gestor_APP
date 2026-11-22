package DTO;

import Model.OrigemMovimentacaoFinanceira;
import Model.TipoMovimentacaoFinanceira;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovimentacaoFinanceiraResponseDTO(
        Long idMovimentacaoFinanceira,
        Long empresaId,
        Long filialId,
        Long contaFinanceiraId,
        String contaFinanceiraNome,
        TipoMovimentacaoFinanceira tipoMovimentacao,
        OrigemMovimentacaoFinanceira origemTipo,
        Long origemId,
        String descricao,
        String documentoReferencia,
        LocalDate dataMovimentacao,
        LocalDateTime dataHoraRegistro,
        BigDecimal valorBruto,
        BigDecimal valorTaxa,
        BigDecimal valorJurosDesconto,
        BigDecimal valorLiquido,
        Long planoContaId,
        String planoContaNome,
        Long centroCustoId,
        String centroCustoNome,
        Long transferenciaParId,
        Long movimentacaoEstornadaId,
        boolean estornado,
        boolean conciliado,
        Long conciliacaoItemId,
        String criadoPor) {
}
