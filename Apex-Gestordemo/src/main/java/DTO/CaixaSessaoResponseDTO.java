package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CaixaSessaoResponseDTO(
        Long idCaixaSessao,
        String operadorLogin,
        Long empresaId,
        Long filialId,
        String status,
        BigDecimal saldoInicial,
        BigDecimal totalVendas,
        BigDecimal totalSuprimentos,
        BigDecimal totalSangrias,
        BigDecimal saldoAtual,
        BigDecimal saldoFinal,
        LocalDateTime abertoEm,
        LocalDateTime fechadoEm,
        List<CaixaMovimentacaoResponseDTO> movimentacoes) {
}
