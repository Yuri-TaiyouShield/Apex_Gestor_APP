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
import Service.AuthorizationService;
import Service.FinancialAuditTrailService;
import Service.FinancialCalculationService;
import Service.FinancialDocumentWorkflowService;
import Service.PrivacyGuardService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/financeiro")
public class FinancialComplianceController {

    private final FinancialCalculationService calculationService;
    private final FinancialDocumentWorkflowService documentWorkflowService;
    private final FinancialAuditTrailService auditTrailService;
    private final TenantScopeService tenantScopeService;
    private final AuthorizationService authorizationService;
    private final PrivacyGuardService privacyGuardService;

    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    public FinancialComplianceController(
            FinancialCalculationService calculationService,
            FinancialDocumentWorkflowService documentWorkflowService,
            FinancialAuditTrailService auditTrailService,
            TenantScopeService tenantScopeService,
            AuthorizationService authorizationService
    ) {
        this(calculationService, documentWorkflowService, auditTrailService,
                tenantScopeService, authorizationService, null);
    }

    @Autowired
    public FinancialComplianceController(
            FinancialCalculationService calculationService,
            FinancialDocumentWorkflowService documentWorkflowService,
            FinancialAuditTrailService auditTrailService,
            TenantScopeService tenantScopeService,
            AuthorizationService authorizationService,
            PrivacyGuardService privacyGuardService
    ) {
        this.calculationService = calculationService;
        this.documentWorkflowService = documentWorkflowService;
        this.auditTrailService = auditTrailService;
        this.tenantScopeService = tenantScopeService;
        this.authorizationService = authorizationService;
        this.privacyGuardService = privacyGuardService != null ? privacyGuardService : new PrivacyGuardService();
    }

    @PostMapping("/calculos/trabalhista")
    public ResponseEntity<FinancialCalculationResponseDTO> calculateLabor(
            @Valid @RequestBody LaborCalculationRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "calcular folha trabalhista");
        authorizationService.require(servletRequest, ApexPermission.FINANCE_MANAGE,
                "Financeiro", "calcular folha trabalhista");
        return ResponseEntity.ok(calculationService.calculateLabor(request, actor(authentication),
                tenantScopeService.empresaId(servletRequest), tenantScopeService.filialId(servletRequest)));
    }

    @PostMapping("/calculos/tributario")
    public ResponseEntity<FinancialCalculationResponseDTO> calculateTaxes(
            @Valid @RequestBody TaxCalculationRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "calcular tributos");
        authorizationService.require(servletRequest, ApexPermission.FINANCE_MANAGE,
                "Financeiro", "calcular tributos");
        return ResponseEntity.ok(calculationService.calculateTaxes(request, actor(authentication),
                tenantScopeService.empresaId(servletRequest), tenantScopeService.filialId(servletRequest)));
    }

    @PostMapping("/calculos/admcalc")
    public ResponseEntity<FinancialCalculationResponseDTO> calculateAdmCalc(
            @Valid @RequestBody AdmCalcRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "calcular indicadores admcalc");
        authorizationService.require(servletRequest, ApexPermission.FINANCE_MANAGE,
                "Financeiro", "calcular indicadores admcalc");
        return ResponseEntity.ok(calculationService.calculateAdmCalc(request, actor(authentication),
                tenantScopeService.empresaId(servletRequest), tenantScopeService.filialId(servletRequest)));
    }

    @GetMapping("/calculos")
    public List<FinancialCalculationResponseDTO> listCalculations(
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "listar calculos financeiros");
        authorizationService.require(servletRequest, ApexPermission.FINANCE_VIEW,
                "Financeiro", "listar calculos financeiros");
        int boundedLimit = Math.min(Math.max(limit, 1), 200);
        return calculationService.listRecent(boundedLimit, tenantScopeService.empresaId(servletRequest),
                tenantScopeService.filialId(servletRequest));
    }

    @PostMapping("/documentos")
    public ResponseEntity<FinancialDocumentDTO> createDocument(
            @Valid @RequestBody FinancialDocumentRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "criar documento financeiro");
        authorizationService.require(servletRequest, ApexPermission.FINANCE_MANAGE,
                "Financeiro", "criar documento financeiro");
        return new ResponseEntity<>(documentWorkflowService.create(request, resolvedAuth(authentication),
                tenantScopeService.empresaId(servletRequest), tenantScopeService.filialId(servletRequest)), HttpStatus.CREATED);
    }

    @GetMapping("/documentos")
    public List<FinancialDocumentDTO> listDocuments(
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "listar documentos financeiros");
        if (!authorizationService.can(servletRequest, ApexPermission.FINANCE_COMPLIANCE_VIEW)
                && !authorizationService.can(servletRequest, ApexPermission.FINANCE_VIEW)) {
            authorizationService.require(servletRequest, ApexPermission.FINANCE_COMPLIANCE_VIEW,
                    "Financeiro", "listar documentos financeiros");
        }
        int boundedLimit = Math.min(Math.max(limit, 1), 200);
        boolean revealPii = authorizationService.can(servletRequest, ApexPermission.PERSONAL_DATA_VIEW)
                && privacyGuardService.canViewSensitiveData(servletRequest);
        return documentWorkflowService.listRecent(boundedLimit, tenantScopeService.empresaId(servletRequest),
                tenantScopeService.filialId(servletRequest), revealPii);
    }

    @PostMapping("/documentos/{id}/rascunho-email")
    public ResponseEntity<FinancialDocumentDTO> createEmailDraft(
            @PathVariable Long id,
            @Valid @RequestBody EmailDraftRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "criar rascunho de email financeiro");
        authorizationService.require(servletRequest, ApexPermission.FINANCE_MANAGE,
                "Financeiro", "criar rascunho de email financeiro");
        return ResponseEntity.ok(documentWorkflowService.createDraft(id, request, resolvedAuth(authentication),
                tenantScopeService.empresaId(servletRequest), tenantScopeService.filialId(servletRequest)));
    }

    @PostMapping("/documentos/{id}/assinaturas")
    public ResponseEntity<FinancialDocumentDTO> signDocument(
            @PathVariable Long id,
            @Valid @RequestBody SignFinancialDocumentRequestDTO request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "assinar documento financeiro");
        Authentication resolved = resolvedAuth(authentication);
        if (!authorizationService.can(servletRequest, ApexPermission.FINANCE_COMPLIANCE_SIGN)
                && !authorizationService.can(servletRequest, ApexPermission.FINANCE_MANAGE)) {
            authorizationService.require(servletRequest, ApexPermission.FINANCE_COMPLIANCE_SIGN,
                    "Financeiro", "assinar documento financeiro");
        }
        return ResponseEntity.ok(documentWorkflowService.sign(id, request, resolved,
                tenantScopeService.empresaId(servletRequest), tenantScopeService.filialId(servletRequest)));
    }

    @GetMapping("/auditoria")
    public List<FinancialAuditEventDTO> listAudit(
            @RequestParam(defaultValue = "100") int limit,
            HttpServletRequest servletRequest) {
        requireCoreLicense(servletRequest, "listar auditoria financeira");
        authorizationService.require(servletRequest, ApexPermission.FINANCE_VIEW,
                "Financeiro", "listar auditoria financeira");
        authorizationService.require(servletRequest, ApexPermission.AUDIT_LOG_VIEW,
                "Financeiro", "listar auditoria financeira");
        int boundedLimit = Math.min(Math.max(limit, 1), 200);
        boolean unmasked = privacyGuardService.canViewUnmaskedAudit(servletRequest);
        return auditTrailService.listRecent(boundedLimit, tenantScopeService.empresaId(servletRequest),
                        tenantScopeService.filialId(servletRequest)).stream()
                .map(dto -> unmasked
                        ? dto
                        : new FinancialAuditEventDTO(
                                dto.idEvento(),
                                dto.tipoEvento(),
                                dto.alvoTipo(),
                                dto.alvoId(),
                                privacyGuardService.maskIdentifier(dto.atorLogin()),
                                dto.valorAnterior(),
                                dto.valorNovo(),
                                dto.metadados(),
                                dto.criadoEm()))
                .toList();
    }

    private void requireCoreLicense(HttpServletRequest request, String acao) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE, "Financeiro", acao);
        }
    }

    private Authentication resolvedAuth(Authentication authentication) {
        return authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
    }

    private String actor(Authentication authentication) {
        Authentication resolved = resolvedAuth(authentication);
        return resolved == null || resolved.getName() == null ? "anonymous" : resolved.getName();
    }
}
