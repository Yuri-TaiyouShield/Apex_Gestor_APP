package DTO;

import Model.TipoContaFinanceira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaFinanceiraRequestDTO(
        @NotBlank String codigo,
        @NotBlank String nome,
        @NotNull TipoContaFinanceira tipoConta,
        String bancoCodigo,
        String bancoNome,
        String agencia,
        String numeroConta,
        BigDecimal saldoInicial,
        LocalDate dataSaldoInicial,
        Boolean padrao,
        Boolean ativo) {
}
