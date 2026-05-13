package DTO;

import Model.Cliente;
import Model.ConsentAudit;
import java.time.LocalDateTime;
import java.util.List;

public record ClienteExportDTO(
        Cliente cliente,
        List<Consentimento> consentimentos,
        LocalDateTime exportadoEm
) {
    public record Consentimento(String versao, String canal, LocalDateTime aceitoEm) {
        public static Consentimento from(ConsentAudit audit) {
            return new Consentimento(audit.getVersao(), audit.getCanal(), audit.getAceitoEm());
        }
    }
}
