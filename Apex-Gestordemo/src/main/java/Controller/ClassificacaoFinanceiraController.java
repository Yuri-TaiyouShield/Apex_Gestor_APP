package Controller;

import DTO.ItemPendenteClassificacaoDTO;
import DTO.PendenciaClassificacaoResponseDTO;
import DTO.ReclassificacaoManualRequestDTO;
import DTO.RegraClassificacaoRequestDTO;
import DTO.RegraClassificacaoResponseDTO;
import DTO.ResultadoClassificacaoLoteDTO;
import Model.OrigemClassificacaoFinanceira;
import Service.AuthorizationService;
import Service.ClassificacaoFinanceiraService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/classificacoes")
public class ClassificacaoFinanceiraController {

    private final ClassificacaoFinanceiraService classificacaoService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    public ClassificacaoFinanceiraController(
            ClassificacaoFinanceiraService classificacaoService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.classificacaoService = classificacaoService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping("/regras")
    public List<RegraClassificacaoResponseDTO> listarRegras(
            @RequestParam(required = false) OrigemClassificacaoFinanceira origemTipo,
            @RequestParam(required = false) Boolean somenteAtivas,
            HttpServletRequest request) {
        requireCoreLicense(request, "listar regras de classificacao");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "listar regras de classificacao");
        return classificacaoService.listarRegras(origemTipo, somenteAtivas,
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request));
    }

    @PostMapping("/regras")
    public ResponseEntity<RegraClassificacaoResponseDTO> criarRegra(
            @Valid @RequestBody RegraClassificacaoRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar classificacao financeira");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "criar regra de classificacao");
        return ResponseEntity.status(HttpStatus.CREATED).body(classificacaoService.criarRegra(
                body, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PutMapping("/regras/{id}")
    public ResponseEntity<RegraClassificacaoResponseDTO> atualizarRegra(
            @PathVariable Long id,
            @Valid @RequestBody RegraClassificacaoRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar classificacao financeira");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "atualizar regra de classificacao");
        return ResponseEntity.ok(classificacaoService.atualizarRegra(
                id, body, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PatchMapping("/regras/{id}/status")
    public ResponseEntity<RegraClassificacaoResponseDTO> alterarStatusRegra(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar classificacao financeira");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "alterar status da regra de classificacao");
        return ResponseEntity.ok(classificacaoService.alterarStatusRegra(
                id, ativo, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @GetMapping("/pendentes")
    public ResponseEntity<PendenciaClassificacaoResponseDTO> pendentes(
            @RequestParam(required = false) OrigemClassificacaoFinanceira origemTipo,
            HttpServletRequest request) {
        requireCoreLicense(request, "consultar pendencias de classificacao");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "consultar pendencias de classificacao");
        return ResponseEntity.ok(classificacaoService.pendentes(
                origemTipo, tenantScopeService.empresaId(request), tenantScopeService.filialId(request)));
    }

    @PostMapping("/aplicar-automatico")
    public ResponseEntity<ResultadoClassificacaoLoteDTO> aplicarAutomatico(
            @RequestParam(required = false) OrigemClassificacaoFinanceira origemTipo,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar classificacao financeira");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "aplicar classificacao automatica");
        return ResponseEntity.ok(classificacaoService.aplicarAutomatico(
                origemTipo, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PostMapping("/manual")
    public ResponseEntity<ItemPendenteClassificacaoDTO> reclassificarManual(
            @Valid @RequestBody ReclassificacaoManualRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar classificacao financeira");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "reclassificar lancamento manualmente");
        return ResponseEntity.ok(classificacaoService.reclassificarManual(
                body, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    private void requireCoreLicense(HttpServletRequest request, String acao) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE, "Financeiro", acao);
        }
    }
}
