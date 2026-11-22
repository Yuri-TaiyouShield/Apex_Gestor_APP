package DTO;

import java.time.LocalDateTime;

public record VendaEventoResponseDTO(Long idVendaEvento, String evento, String statusAnterior,
        String statusNovo, String motivo, String usuarioLogin, LocalDateTime criadoEm) {
}
