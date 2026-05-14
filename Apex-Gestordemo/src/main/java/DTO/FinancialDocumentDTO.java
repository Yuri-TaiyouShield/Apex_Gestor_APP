package DTO;

import java.time.LocalDateTime;

public record FinancialDocumentDTO(
        Long idDocumento,
        String tipoDocumento,
        String funcionarioNome,
        String funcionarioEmail,
        String status,
        String cargoAssinanteObrigatorio,
        String geradoPor,
        LocalDateTime geradoEm,
        String assinadoPor,
        String cargoAssinante,
        LocalDateTime assinadoEm,
        LocalDateTime enviadoEm,
        String assinaturaDigitalHash,
        String assuntoEmail,
        String mensagemEmail,
        String referencia
) {
}
