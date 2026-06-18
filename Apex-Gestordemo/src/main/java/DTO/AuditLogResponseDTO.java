package DTO;

import java.time.LocalDateTime;

public record AuditLogResponseDTO(
        Long idAuditoria,
        String usuarioLogin,
        Long empresaId,
        Long filialId,
        String modulo,
        String acao,
        String entidade,
        Long entidadeId,
        String ip,
        String dispositivo,
        String traceId,
        String justificativa,
        LocalDateTime criadoEm) {
}
