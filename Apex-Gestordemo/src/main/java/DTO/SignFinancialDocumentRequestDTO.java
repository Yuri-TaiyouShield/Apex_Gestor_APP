package DTO;

import jakarta.validation.constraints.NotBlank;

public record SignFinancialDocumentRequestDTO(
        @NotBlank String nomeAssinante,
        @NotBlank String cargoAssinante,
        String emailAssinante,
        String certificadoFingerprint,
        boolean aprovado,
        String observacao
) {
}
