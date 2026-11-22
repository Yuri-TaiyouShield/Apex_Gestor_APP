package DTO;

public record FormaPagamentoResponseDTO(
        Long idFormaPagamento,
        String nome,
        String descricao,
        Integer tipoPagamento,
        Integer status,
        Long empresaId,
        Long filialId) {
}
