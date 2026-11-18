package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportacaoExtratoRequestDTO(
        @NotNull Long contaFinanceiraId,
        @NotBlank String tipoConciliacao,
        @NotBlank String formatoOrigem,
        String conteudoArquivo,
        List<LinhaExtratoManualDTO> linhas) {

    public record LinhaExtratoManualDTO(
            String identificadorExtrato,
            LocalDate dataExtrato,
            String descricaoExtrato,
            BigDecimal valorExtrato,
            String naturezaExtrato) {
    }
}
