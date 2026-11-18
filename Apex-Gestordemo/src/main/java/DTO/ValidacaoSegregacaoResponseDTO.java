package DTO;

import java.math.BigDecimal;

public record ValidacaoSegregacaoResponseDTO(
        boolean autorizado,
        String dominioOperacao,
        Long submetidoPorUsuarioId,
        Long aprovadoPorUsuarioId,
        boolean autoaprovacaoUnipessoal,
        long aprovadoresAtivosNaEmpresa,
        BigDecimal valorOperacao,
        BigDecimal alcadaAplicada,
        String parecerValidacao
) {
}
