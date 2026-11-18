package DTO;

import Model.TipoMovimentacaoEstoque;
import java.time.LocalDateTime;

public record EstoqueMovimentacaoResponseDTO(
        Long idMovimentacao,
        Long produtoId,
        String produtoDescricao,
        TipoMovimentacaoEstoque tipo,
        Integer quantidade,
        Integer saldoAnterior,
        Integer saldoPosterior,
        String referenciaTipo,
        Long referenciaId,
        String motivo,
        Long usuarioId,
        Long empresaId,
        Long filialId,
        LocalDateTime criadoEm) {
}
