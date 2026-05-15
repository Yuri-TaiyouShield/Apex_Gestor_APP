package DTO;

public record CatalogEnrichmentResultDTO(
        Long produtoId,
        String status,
        String imagemUrl,
        String message
) {
}
