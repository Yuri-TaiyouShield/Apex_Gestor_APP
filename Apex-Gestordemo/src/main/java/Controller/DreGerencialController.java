package Controller;

import DTO.DreComparativoFiliaisResponseDTO;
import DTO.DreDrillDownResponseDTO;
import DTO.DreEvolucaoResponseDTO;
import DTO.DreGerencialResponseDTO;
import Model.GrupoDreGerencial;
import Service.AuthorizationService;
import Service.DreGerencialService;
import Service.DreGerencialService.FiltroDreRequest;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/dre")
public class DreGerencialController {

    private final DreGerencialService dreGerencialService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    public DreGerencialController(
            DreGerencialService dreGerencialService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.dreGerencialService = dreGerencialService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping
    public ResponseEntity<DreGerencialResponseDTO> apurarDre(
            @RequestParam(required = false) String tipoPeriodo,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Boolean compararPeriodoAnterior,
            @RequestParam(required = false) String competenciaComparacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate comparacaoInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate comparacaoFim,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long filialFiltroId,
            @RequestParam(required = false) Boolean consolidarFiliais,
            Authentication authentication,
            HttpServletRequest request) {
        if (!authorizationService.can(request, ApexPermission.FINANCE_DRE_VIEW)
                && !authorizationService.can(request, ApexPermission.FINANCE_VIEW)) {
            authorizationService.require(request, ApexPermission.FINANCE_DRE_VIEW,
                    "Financeiro", "consultar DRE gerencial");
        }
        FiltroDreRequest filtro = new FiltroDreRequest(
                tipoPeriodo,
                competencia,
                ano,
                trimestre,
                semestre,
                dataInicio,
                dataFim,
                compararPeriodoAnterior,
                competenciaComparacao,
                comparacaoInicio,
                comparacaoFim,
                centroCustoId,
                filialFiltroId,
                consolidarFiliais);
        return ResponseEntity.ok(dreGerencialService.apurarDre(
                filtro,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @GetMapping("/lancamentos")
    public ResponseEntity<DreDrillDownResponseDTO> detalharLancamentos(
            @RequestParam(required = false) String tipoPeriodo,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long filialFiltroId,
            @RequestParam(required = false) Boolean consolidarFiliais,
            @RequestParam(required = false) Integer numeroLinha,
            @RequestParam(required = false) String codigoLinha,
            @RequestParam(required = false) String grupoDre,
            @RequestParam(required = false) Long planoContaId,
            Authentication authentication,
            HttpServletRequest request) {
        if (!authorizationService.can(request, ApexPermission.FINANCE_DRE_VIEW)
                && !authorizationService.can(request, ApexPermission.FINANCE_VIEW)) {
            authorizationService.require(request, ApexPermission.FINANCE_DRE_VIEW,
                    "Financeiro", "consultar DRE gerencial");
        }
        GrupoDreGerencial grupoEnum = parseGrupoDre(grupoDre);
        FiltroDreRequest filtro = new FiltroDreRequest(
                tipoPeriodo,
                competencia,
                ano,
                trimestre,
                semestre,
                dataInicio,
                dataFim,
                false,
                null,
                null,
                null,
                centroCustoId,
                filialFiltroId,
                consolidarFiliais);
        return ResponseEntity.ok(dreGerencialService.detalharLancamentos(
                filtro,
                numeroLinha,
                codigoLinha,
                planoContaId,
                grupoEnum,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @GetMapping("/evolucao")
    public ResponseEntity<DreEvolucaoResponseDTO> evoluirDre(
            @RequestParam(required = false) String competenciaInicio,
            @RequestParam(required = false) String competenciaFim,
            @RequestParam(required = false) Integer meses,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long filialFiltroId,
            @RequestParam(required = false) Boolean consolidarFiliais,
            Authentication authentication,
            HttpServletRequest request) {
        if (!authorizationService.can(request, ApexPermission.FINANCE_DRE_VIEW)
                && !authorizationService.can(request, ApexPermission.FINANCE_VIEW)) {
            authorizationService.require(request, ApexPermission.FINANCE_DRE_VIEW,
                    "Financeiro", "consultar DRE gerencial");
        }
        return ResponseEntity.ok(dreGerencialService.evoluirDre(
                competenciaInicio,
                competenciaFim,
                meses,
                centroCustoId,
                filialFiltroId,
                consolidarFiliais,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @GetMapping("/comparativo-filiais")
    public ResponseEntity<DreComparativoFiliaisResponseDTO> compararFiliais(
            @RequestParam(required = false) String tipoPeriodo,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long centroCustoId,
            Authentication authentication,
            HttpServletRequest request) {
        if (!authorizationService.can(request, ApexPermission.FINANCE_DRE_VIEW)
                && !authorizationService.can(request, ApexPermission.FINANCE_VIEW)) {
            authorizationService.require(request, ApexPermission.FINANCE_DRE_VIEW,
                    "Financeiro", "consultar DRE gerencial");
        }
        FiltroDreRequest filtro = new FiltroDreRequest(
                tipoPeriodo,
                competencia,
                ano,
                trimestre,
                semestre,
                dataInicio,
                dataFim,
                false,
                null,
                null,
                null,
                centroCustoId,
                null,
                true);
        return ResponseEntity.ok(dreGerencialService.compararFiliais(
                filtro,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @GetMapping("/exportar")
    public ResponseEntity<?> exportarDre(
            @RequestParam(required = false, defaultValue = "CSV") String formato,
            @RequestParam(required = false) String tipoPeriodo,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Boolean compararPeriodoAnterior,
            @RequestParam(required = false) String competenciaComparacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate comparacaoInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate comparacaoFim,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long filialFiltroId,
            @RequestParam(required = false) Boolean consolidarFiliais,
            Authentication authentication,
            HttpServletRequest request) {
        if (!authorizationService.can(request, ApexPermission.REPORT_EXPORT)
                && !authorizationService.can(request, ApexPermission.FINANCE_MANAGE)) {
            authorizationService.require(request, ApexPermission.REPORT_EXPORT,
                    "Financeiro", "exportar DRE gerencial");
        }
        Long empresaId = tenantScopeService.empresaId(request);
        Long filialId = tenantScopeService.filialId(request);
        FiltroDreRequest filtro = new FiltroDreRequest(
                tipoPeriodo,
                competencia,
                ano,
                trimestre,
                semestre,
                dataInicio,
                dataFim,
                compararPeriodoAnterior,
                competenciaComparacao,
                comparacaoInicio,
                comparacaoFim,
                centroCustoId,
                filialFiltroId,
                consolidarFiliais);
        String fmt = formato == null ? "CSV" : formato.trim().toUpperCase(Locale.ROOT);

        if ("JSON".equals(fmt)) {
            DreGerencialResponseDTO dto = dreGerencialService.exportarJsonAuditado(
                    filtro,
                    empresaId,
                    filialId,
                    request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDisposition(ContentDisposition.attachment().filename("dre-gerencial.json").build());
            return ResponseEntity.ok().headers(headers).body(dto);
        }

        if (!"CSV".equals(fmt)) {
            throw new IllegalArgumentException("Formato de exportacao invalido. Utilize CSV ou JSON.");
        }

        String csv = dreGerencialService.exportarCsv(
                filtro,
                empresaId,
                filialId,
                request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("dre-gerencial.csv").build());
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    private GrupoDreGerencial parseGrupoDre(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return GrupoDreGerencial.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Grupo da DRE invalido: " + raw);
        }
    }
}
