package DTO;

import java.util.List;

public record LgpdSimulacaoAnonimizacaoDTO(
        Long clienteId,
        boolean elegivelAnonimizacaoPessoal,
        boolean bloqueioOperacionalAtivo,
        boolean bloqueioExclusaoFisicaPorRetencaoLegal,
        long vendasVinculadas,
        long contasReceberEmAberto,
        long creditosAtivos,
        List<String> obrigacoesRetencao,
        List<String> camposAnonimizados,
        List<String> camposPreservadosPorObrigacaoLegal,
        boolean exigeAprovacaoSegundoResponsavel,
        long responsaveisElegiveisEmpresa,
        String simulacaoHash
) {
}
