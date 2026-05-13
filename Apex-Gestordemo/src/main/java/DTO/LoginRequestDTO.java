package DTO;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank String login,
        @NotBlank String senha,
        String totpCode,
        String consentVersion,
        Boolean acceptedPrivacyTerms
) {
}
