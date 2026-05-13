package DTO;

import java.time.Instant;
import java.util.List;

public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        String tokenType,
        Long usuarioId,
        String nome,
        String login,
        List<String> roles
) {
}
