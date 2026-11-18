package Controller;

import DTO.AplicarPrecoProdutoRequestDTO;
import DTO.BalancoPatrimonialGerencialRequestDTO;
import DTO.BalancoPatrimonialGerencialResponseDTO;
import DTO.InvestimentosRetornoPainelDTO;
import DTO.PainelInteligenciaAdministrativaResponseDTO;
import DTO.ParametroInteligenciaAdministrativaDTO;
import DTO.ProdutoResponseDTO;
import DTO.SimulacaoInvestimentoRequestDTO;
import DTO.SimulacaoPrecificacaoRequestDTO;
import DTO.SimulacaoPrecificacaoResponseDTO;
import Service.AuthorizationService;
import Service.IndicadoresAdministrativosService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/indicadores")
public class IndicadoresAdministrativosController {

    private final IndicadoresAdministrativosService indicadoresService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    public IndicadoresAdministrativosController(
            IndicadoresAdministrativosService indicadoresService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.indicadoresService = indicadoresService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping
    public ResponseEntity<PainelInteligenciaAdministrativaResponseDTO> consultarPainelConsolidado(
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long filialId,
            @RequestParam(required = false) Boolean consolidarFiliais,
            HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(indicadoresService.consultarPainelConsolidado(
                competencia,
                dataInicio,
                dataFim,
                filialId,
                consolidarFiliais,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @GetMapping("/parametros")
    public ResponseEntity<ParametroInteligenciaAdministrativaDTO> consultarParametros(HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(indicadoresService.consultarParametros(
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @PutMapping("/parametros")
    public ResponseEntity<ParametroInteligenciaAdministrativaDTO> salvarParametros(
            @Valid @RequestBody ParametroInteligenciaAdministrativaDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "atualizar parametros e benchmarks de indicadores administrativos");
        return ResponseEntity.ok(indicadoresService.salvarParametros(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @GetMapping("/balanco-patrimonial")
    public ResponseEntity<BalancoPatrimonialGerencialResponseDTO> consultarBalancoPatrimonial(
            @RequestParam(required = false) String competencia,
            HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(indicadoresService.consultarBalancoPatrimonial(
                competencia,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @PutMapping("/balanco-patrimonial")
    public ResponseEntity<BalancoPatrimonialGerencialResponseDTO> salvarBalancoPatrimonial(
            @Valid @RequestBody BalancoPatrimonialGerencialRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "salvar e confirmar Balanco Patrimonial Gerencial");
        return ResponseEntity.ok(indicadoresService.salvarBalancoPatrimonial(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping("/precificacao/simular")
    public ResponseEntity<SimulacaoPrecificacaoResponseDTO> simularPrecificacao(
            @Valid @RequestBody SimulacaoPrecificacaoRequestDTO body,
            HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(indicadoresService.simularPrecificacao(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @PostMapping("/precificacao/aplicar-preco")
    public ResponseEntity<ProdutoResponseDTO> aplicarPrecoProduto(
            @Valid @RequestBody AplicarPrecoProdutoRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "aplicar preco calculado no cadastro do produto");
        return ResponseEntity.ok(indicadoresService.aplicarPrecoProdutoAutorizado(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping("/investimentos/simular")
    public ResponseEntity<InvestimentosRetornoPainelDTO> simularInvestimento(
            @Valid @RequestBody SimulacaoInvestimentoRequestDTO body,
            @RequestParam(required = false) String competencia,
            HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(indicadoresService.simularInvestimento(
                body,
                competencia,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    private void requireReadPermission(HttpServletRequest request) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE,
                    "Financeiro", "consultar motor de indicadores administrativos");
        }
        authorizationService.require(request, ApexPermission.FINANCIAL_INTELLIGENCE_VIEW,
                "Financeiro", "consultar motor de indicadores administrativos");
    }

    private void requireManagePermission(HttpServletRequest request, String acao) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE, "Financeiro", acao);
        }
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", acao);
    }
}
