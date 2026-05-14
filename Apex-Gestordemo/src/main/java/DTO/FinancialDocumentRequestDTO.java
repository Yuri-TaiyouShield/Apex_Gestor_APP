package DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FinancialDocumentRequestDTO(
        @NotBlank String tipoDocumento,
        @NotBlank String funcionarioNome,
        @Email @NotBlank String funcionarioEmail,
        String cargoAssinanteObrigatorio,
        @NotBlank String conteudo,
        String referencia
) {
}
