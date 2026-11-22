package DTO;

import java.math.BigDecimal;
import java.util.List;

public record ConciliacaoResumoResponseDTO(
        long totalItensExtrato,
        long conciliadosExatos,
        long conciliadosManuais,
        long pendentes,
        long divergentes,
        long duplicidadesSuspeitas,
        BigDecimal valorTotalDivergente,
        int sessoesCaixaComDiferencaFechamento,
        BigDecimal diferencaTotalFechamentoCaixa,
        List<ConciliacaoItemResponseDTO> itens) {
}
