package DTO;

import java.time.LocalDateTime;

public record ConsentimentoResponseDTO(
        Long consentId,
        String titularId,
        String tipoTitular,
        String finalidade,
        String versao,
        String status,
        LocalDateTime registeredAt) {
}
