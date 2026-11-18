package DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PainelInteligenciaAdministrativaResponseDTO(
        String competencia,
        LocalDate dataInicio,
        LocalDate dataFim,
        Long empresaId,
        Long filialId,
        String filialNome,
        boolean consolidadoEmpresa,
        LocalDateTime geradoEm,
        ParametroInteligenciaAdministrativaDTO parametrosVigentes,
        BalancoPatrimonialGerencialResponseDTO balancoPatrimonial,
        RentabilidadePainelDTO rentabilidade,
        PontoEquilibrioPainelDTO pontoEquilibrio,
        SimulacaoPrecificacaoResponseDTO precificacaoPadrao,
        CapitalGiroPainelDTO capitalGiro,
        PrazosCiclosPainelDTO prazosCiclos,
        LiquidezEndividamentoPainelDTO liquidezEndividamento,
        InvestimentosRetornoPainelDTO investimentosRetorno,
        List<IndicadorDetalhadoDTO> catalogoIndicadores,
        List<DiagnosticoAdministrativoItemDTO> diagnosticosExplicaveis) {
}
