package Controller;

import DTO.RelatorioFinanceiroDTO;
import DTO.RelatorioOperacionalDTO;
import Service.AuthorizationService;
import Service.FinanceiroService;
import Service.RelatorioOperacionalService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    @Autowired
    private FinanceiroService financeiroService;
    @Autowired
    private RelatorioOperacionalService relatorioOperacionalService;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    @GetMapping("/financeiro")
    public ResponseEntity<RelatorioFinanceiroDTO> getFinanceiro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerar relatorio financeiro");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Relatorios", "relatorio financeiro");
        return ResponseEntity.ok(financeiroService.gerarRelatorio(
                inicio,
                fim,
                empresaId(request),
                filialId(request)));
    }

    @GetMapping("/clientes")
    public ResponseEntity<RelatorioOperacionalDTO> clientes(HttpServletRequest request) {
        requireCoreLicense(request, "relatorio de clientes");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Relatorios", "relatorio de clientes");
        return ResponseEntity.ok(relatorioOperacionalService.clientes(empresaId(request), filialId(request)));
    }

    @GetMapping("/estoque")
    public ResponseEntity<RelatorioOperacionalDTO> estoque(HttpServletRequest request) {
        requireCoreLicense(request, "relatorio de estoque");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Relatorios", "relatorio de estoque");
        return ResponseEntity.ok(relatorioOperacionalService.estoque(empresaId(request), filialId(request)));
    }

    @GetMapping("/funcionarios")
    public ResponseEntity<RelatorioOperacionalDTO> funcionarios(HttpServletRequest request) {
        requireCoreLicense(request, "relatorio de funcionarios");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Relatorios", "relatorio de funcionarios");
        return ResponseEntity.ok(relatorioOperacionalService.funcionarios(empresaId(request), filialId(request)));
    }

    @GetMapping("/vendas")
    public ResponseEntity<RelatorioOperacionalDTO> vendas(HttpServletRequest request) {
        requireCoreLicense(request, "relatorio de vendas");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Relatorios", "relatorio de vendas");
        return ResponseEntity.ok(relatorioOperacionalService.vendas(empresaId(request), filialId(request)));
    }

    @GetMapping("/fornecedores")
    public ResponseEntity<RelatorioOperacionalDTO> fornecedores(HttpServletRequest request) {
        requireCoreLicense(request, "relatorio de fornecedores");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Relatorios", "relatorio de fornecedores");
        return ResponseEntity.ok(relatorioOperacionalService.fornecedores(empresaId(request), filialId(request)));
    }

    private void requireCoreLicense(HttpServletRequest request, String acao) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE, "Relatorios", acao);
        }
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
