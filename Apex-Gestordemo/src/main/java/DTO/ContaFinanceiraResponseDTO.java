package DTO;

import Model.TipoContaFinanceira;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContaFinanceiraResponseDTO(
        Long idContaFinanceira,
        Long empresaId,
        Long filialId,
        String filialNome,
        String codigo,
        String nome,
        TipoContaFinanceira tipoConta,
        String bancoCodigo,
        String bancoNome,
        String agencia,
        String numeroConta,
        BigDecimal saldoInicial,
        BigDecimal saldoAtual,
        LocalDate dataSaldoInicial,
        boolean padrao,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
