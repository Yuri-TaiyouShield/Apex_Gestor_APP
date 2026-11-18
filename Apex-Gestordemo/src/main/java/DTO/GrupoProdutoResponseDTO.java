package DTO;

public record GrupoProdutoResponseDTO(
        Long idGrupoProduto,
        String nome,
        String descricao,
        Integer status,
        Long empresaId,
        Long filialId) {
}
