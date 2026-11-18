package Controller;

import DTO.PlanoContasBootstrapResponseDTO;
import DTO.PlanoContasRequestDTO;
import DTO.PlanoContasResponseDTO;
import Model.GrupoDreGerencial;
import Model.TipoContaGerencial;
import Service.AuthorizationService;
import Service.PlanoContasService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/financeiro/plano-contas")
public class PlanoContasController {

    private final PlanoContasService planoContasService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    public PlanoContasController(
            PlanoContasService planoContasService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.planoContasService = planoContasService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping
    public List<PlanoContasResponseDTO> listar(
            @RequestParam(required = false) Boolean somenteAtivos,
            @RequestParam(required = false) GrupoDreGerencial grupoDre,
            @RequestParam(required = false) TipoContaGerencial tipoConta,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "listar plano de contas");
        return planoContasService.listar(somenteAtivos, grupoDre, tipoConta,
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request));
    }

    @GetMapping("/arvore")
    public List<PlanoContasResponseDTO> arvore(
            @RequestParam(required = false) Boolean somenteAtivos,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "consultar arvore do plano de contas");
        return planoContasService.arvore(somenteAtivos,
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanoContasResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "consultar conta gerencial");
        return ResponseEntity.ok(planoContasService.buscarPorId(
                id, tenantScopeService.empresaId(request), tenantScopeService.filialId(request)));
    }

    @PostMapping("/inicializar-padrao")
    public ResponseEntity<PlanoContasBootstrapResponseDTO> inicializarPadrao(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "inicializar plano de contas padrao");
        return ResponseEntity.ok(planoContasService.inicializarPadrao(
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PostMapping
    public ResponseEntity<PlanoContasResponseDTO> criar(
            @Valid @RequestBody PlanoContasRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "criar conta gerencial");
        return ResponseEntity.status(HttpStatus.CREATED).body(planoContasService.criar(
                body, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanoContasResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlanoContasRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "atualizar conta gerencial");
        return ResponseEntity.ok(planoContasService.atualizar(
                id, body, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PlanoContasResponseDTO> alterarStatus(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "alterar status da conta gerencial");
        return ResponseEntity.ok(planoContasService.alterarStatus(
                id, ativo, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }
}
