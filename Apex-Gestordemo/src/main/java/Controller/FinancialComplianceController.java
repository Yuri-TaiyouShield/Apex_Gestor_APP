package Controller;

import DTO.AdmCalcRequestDTO;
import DTO.EmailDraftRequestDTO;
import DTO.FinancialAuditEventDTO;
import DTO.FinancialCalculationResponseDTO;
import DTO.FinancialDocumentDTO;
import DTO.FinancialDocumentRequestDTO;
import DTO.LaborCalculationRequestDTO;
import DTO.SignFinancialDocumentRequestDTO;
import DTO.TaxCalculationRequestDTO;
import Service.FinancialAuditTrailService;
import Service.FinancialCalculationService;
import Service.FinancialDocumentWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/financeiro")
@CrossOrigin(originPatterns = {"http://localhost:*", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class FinancialComplianceController {

    private final FinancialCalculationService calculationService;
    private final FinancialDocumentWorkflowService documentWorkflowService;
    private final FinancialAuditTrailService auditTrailService;

    public FinancialComplianceController(
            FinancialCalculationService calculationService,
            FinancialDocumentWorkflowService documentWorkflowService,
            FinancialAuditTrailService auditTrailService
    ) {
        this.calculationService = calculationService;
        this.documentWorkflowService = documentWorkflowService;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping("/calculos/trabalhista")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS')")
    public ResponseEntity<FinancialCalculationResponseDTO> calculateLabor(@Valid @RequestBody LaborCalculationRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(calculationService.calculateLabor(request, authentication.getName()));
    }

    @PostMapping("/calculos/tributario")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS')")
    public ResponseEntity<FinancialCalculationResponseDTO> calculateTaxes(@RequestBody TaxCalculationRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(calculationService.calculateTaxes(request, authentication.getName()));
    }

    @PostMapping("/calculos/admcalc")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS')")
    public ResponseEntity<FinancialCalculationResponseDTO> calculateAdmCalc(@RequestBody AdmCalcRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(calculationService.calculateAdmCalc(request, authentication.getName()));
    }

    @GetMapping("/calculos")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS')")
    public List<FinancialCalculationResponseDTO> listCalculations(@RequestParam(defaultValue = "50") int limit) {
        return calculationService.listRecent(limit);
    }

    @PostMapping("/documentos")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS')")
    public ResponseEntity<FinancialDocumentDTO> createDocument(@Valid @RequestBody FinancialDocumentRequestDTO request, Authentication authentication) {
        return new ResponseEntity<>(documentWorkflowService.create(request, authentication), HttpStatus.CREATED);
    }

    @GetMapping("/documentos")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS','CONTADOR','ADVOGADO')")
    public List<FinancialDocumentDTO> listDocuments(@RequestParam(defaultValue = "50") int limit) {
        return documentWorkflowService.listRecent(limit);
    }

    @PostMapping("/documentos/{id}/rascunho-email")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS')")
    public ResponseEntity<FinancialDocumentDTO> createEmailDraft(@PathVariable Long id, @Valid @RequestBody EmailDraftRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(documentWorkflowService.createDraft(id, request, authentication));
    }

    @PostMapping("/documentos/{id}/assinaturas")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS','CONTADOR','ADVOGADO')")
    public ResponseEntity<FinancialDocumentDTO> signDocument(@PathVariable Long id, @Valid @RequestBody SignFinancialDocumentRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(documentWorkflowService.sign(id, request, authentication));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRACAO','FINANCEIRO','FINANCAS')")
    public List<FinancialAuditEventDTO> listAudit(@RequestParam(defaultValue = "100") int limit) {
        return auditTrailService.listRecent(limit);
    }
}
