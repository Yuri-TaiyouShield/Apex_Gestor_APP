package DTO;

import java.time.LocalDateTime;

public record TitularDataResponseDTO(
        Long requestId,
        String titularId,
        String tipoTitular,
        String tipo,
        String status,
        String justificativa,
        String solicitadoPor,
        LocalDateTime solicitadoEm,
        LocalDateTime executadoEm) {
}
