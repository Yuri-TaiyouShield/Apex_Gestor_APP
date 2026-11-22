package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FluxoCaixaSimulacaoRequestDTO(
        Integer horizonteDias,
        BigDecimal entradaSimuladaAdicional,
        BigDecimal saidaSimuladaAdicional,
        BigDecimal investimentoPlanejado,
        LocalDate dataSimulacao,
        Boolean simularAntecipacaoCartao,
        BigDecimal taxaAntecipacaoSimuladaPercentual,
        List<ItemSimulacaoManualDTO> itensSimulados) {

    public record ItemSimulacaoManualDTO(
            String descricao,
            String natureza,
            BigDecimal valor,
            LocalDate dataPrevista) {
    }
}
