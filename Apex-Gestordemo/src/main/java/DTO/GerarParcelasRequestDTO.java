package DTO;

import Model.ModalidadeRecebivel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GerarParcelasRequestDTO(
        @NotBlank String natureza,
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valorTotal,
        @Min(1) int quantidadeParcelas,
        @Min(1) Integer intervaloDias,
        @NotNull LocalDate primeiroVencimento,
        LocalDate dataCompetencia,
        Long vendaId,
        Long clienteId,
        Long fornecedorId,
        Long categoriaId,
        Long formaPagamentoId,
        Long planoContaId,
        Long centroCustoId,
        Long contaFinanceiraId,
        ModalidadeRecebivel modalidadeRecebivel,
        BigDecimal taxaPercentualCartao,
        Integer prazoRepasseDias,
        Boolean recorrente,
        String periodicidade) {
}
