package DTO;

public record CategoriaResponseDTO(
        Long idCategoria,
        String descricao,
        Integer status,
        Long empresaId,
        Long filialId) {
}
