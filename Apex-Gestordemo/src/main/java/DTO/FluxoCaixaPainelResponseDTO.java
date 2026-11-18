package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FluxoCaixaPainelResponseDTO(
        LocalDate dataInicio,
        LocalDate dataFim,
        Long empresaId,
        Long filialId,
        String filialNome,
        boolean consolidadoEmpresa,
        Long contaFinanceiraFiltroId,
        Long centroCustoFiltroId,
        // 1. Caixa Realizado
        BigDecimal saldoInicialRealizado,
        BigDecimal entradasRealizadasPeriodo,
        BigDecimal saidasRealizadasPeriodo,
        BigDecimal geracaoLiquidaCaixaRealizado,
        BigDecimal saldoFinalRealizado,
        BigDecimal transferenciasInternasVolume,
        BigDecimal totalTaxasCartaoDescontadas,
        BigDecimal totalEstornosPeriodo,
        // 2. Caixa Previsto
        BigDecimal entradasPrevistasPendentes,
        BigDecimal entradasPrevistasVencidas,
        BigDecimal saidasPrevistasPendentes,
        BigDecimal saidasPrevistasVencidas,
        BigDecimal saldoPrevistoLiquido30Dias,
        // 3. Listas e Series
        List<ContaFinanceiraResponseDTO> contasFinanceiras,
        List<MovimentacaoFinanceiraResponseDTO> movimentacoesRealizadas,
        List<FluxoCaixaPrevistoItemDTO> itensPrevistos,
        List<FluxoCaixaDiaCalendarioDTO> calendarioFinanceiro,
        List<FluxoCaixaCenarioProjecaoDTO> cenariosProjecao,
        DiagnosticoLiquidezDTO diagnosticoLiquidez,
        ConciliacaoResumoResponseDTO resumoConciliacao) {
}
