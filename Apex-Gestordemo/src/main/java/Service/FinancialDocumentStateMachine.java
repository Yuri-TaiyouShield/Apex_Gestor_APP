package Service;

import Model.FinancialDocumentStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class FinancialDocumentStateMachine {

    private static final Map<FinancialDocumentStatus, Set<FinancialDocumentStatus>> TRANSITIONS = Map.of(
            FinancialDocumentStatus.DRAFT, Set.of(FinancialDocumentStatus.PENDING_SIGNATURE),
            FinancialDocumentStatus.PENDING_SIGNATURE, Set.of(FinancialDocumentStatus.SIGNED, FinancialDocumentStatus.SIGNATURE_FAILED),
            FinancialDocumentStatus.SIGNED, Set.of(FinancialDocumentStatus.SENT),
            FinancialDocumentStatus.SIGNATURE_FAILED, Set.of(FinancialDocumentStatus.PENDING_SIGNATURE),
            FinancialDocumentStatus.SENT, Set.of()
    );

    public String transition(String currentStatus, FinancialDocumentStatus targetStatus) {
        FinancialDocumentStatus current = parse(currentStatus);
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(targetStatus)) {
            throw new IllegalStateException("Transicao de documento invalida: " + current + " -> " + targetStatus);
        }
        return targetStatus.name();
    }

    public FinancialDocumentStatus parse(String status) {
        if (status == null || status.isBlank()) {
            return FinancialDocumentStatus.DRAFT;
        }
        return switch (status) {
            case "PENDENTE_ASSINATURA" -> FinancialDocumentStatus.PENDING_SIGNATURE;
            case "ASSINADO" -> FinancialDocumentStatus.SIGNED;
            case "ENVIADO" -> FinancialDocumentStatus.SENT;
            default -> FinancialDocumentStatus.valueOf(status);
        };
    }
}
