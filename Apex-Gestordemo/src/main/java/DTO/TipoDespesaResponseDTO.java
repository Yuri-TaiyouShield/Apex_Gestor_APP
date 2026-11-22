package DTO;

public record TipoDespesaResponseDTO(
        Long idTipoDespesa,
        String nome,
        Integer status,
        Long empresaId,
        Long filialId,
        String naturezaDespesa,
        Long planoContaId,
        String planoContaCodigo,
        String planoContaNome,
        Long centroCustoId,
        String centroCustoCodigo,
        String centroCustoNome) {
}
