package Model;

public record AuditRevisionContext(
        String actor,
        String sourceIp,
        String userAgent,
        String correlationId
) {
}
