package Controller;

import DTO.BaixaTituloFinanceiroRequestDTO;
import DTO.ConciliacaoItemResponseDTO;
import DTO.ConciliacaoManualRequestDTO;
import DTO.ConciliacaoResumoResponseDTO;
import DTO.ContaFinanceiraRequestDTO;
import DTO.ContaFinanceiraResponseDTO;
import DTO.EstornoMovimentacaoRequestDTO;
import DTO.FluxoCaixaCenarioProjecaoDTO;
import DTO.FluxoCaixaPainelResponseDTO;
import DTO.FluxoCaixaPrevistoItemDTO;
import DTO.FluxoCaixaSimulacaoRequestDTO;
import DTO.GerarParcelasRequestDTO;
import DTO.ImportacaoExtratoRequestDTO;
import DTO.MovimentacaoFinanceiraResponseDTO;
import DTO.TransferenciaInternaRequestDTO;
import Model.StatusConciliacaoFinanceira;
import Service.AuthorizationService;
import Service.FluxoCaixaService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/fluxo-caixa")
public class FluxoCaixaController {

    private final FluxoCaixaService fluxoCaixaService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    public FluxoCaixaController(
            FluxoCaixaService fluxoCaixaService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.fluxoCaixaService = fluxoCaixaService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping
    public ResponseEntity<FluxoCaixaPainelResponseDTO> consultarPainel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long contaFinanceiraId,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long filialFiltroId,
            @RequestParam(required = false) Boolean consolidarFiliais,
            HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(fluxoCaixaService.consultarPainelFluxoCaixa(
                dataInicio,
                dataFim,
                contaFinanceiraId,
                centroCustoId,
                filialFiltroId,
                consolidarFiliais,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @PostMapping("/simular")
    public ResponseEntity<List<FluxoCaixaCenarioProjecaoDTO>> simularProjecao(
            @RequestBody(required = false) FluxoCaixaSimulacaoRequestDTO body,
            @RequestParam(required = false) Long contaFinanceiraId,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long filialFiltroId,
            @RequestParam(required = false) Boolean consolidarFiliais,
            HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(fluxoCaixaService.simularProjecao(
                body,
                contaFinanceiraId,
                centroCustoId,
                filialFiltroId,
                consolidarFiliais,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @GetMapping("/contas")
    public ResponseEntity<List<ContaFinanceiraResponseDTO>> listarContas(
            @RequestParam(required = false) Boolean somenteAtivas,
            @RequestParam(required = false) Boolean consolidarFiliais,
            HttpServletRequest request) {
        requireReadPermission(request);
        return ResponseEntity.ok(fluxoCaixaService.listarContas(
                somenteAtivas,
                consolidarFiliais,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @PostMapping({"/contas/inicializar-padrao", "/contas/bootstrap"})
    public ResponseEntity<List<ContaFinanceiraResponseDTO>> inicializarContasPadrao(HttpServletRequest request) {
        requireManagePermission(request, "inicializar contas financeiras padrao");
        return ResponseEntity.ok(fluxoCaixaService.inicializarContasPadrao(
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping("/contas")
    public ResponseEntity<ContaFinanceiraResponseDTO> criarConta(
            @Valid @RequestBody ContaFinanceiraRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "criar conta financeira");
        return ResponseEntity.status(HttpStatus.CREATED).body(fluxoCaixaService.criarConta(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PutMapping("/contas/{id}")
    public ResponseEntity<ContaFinanceiraResponseDTO> atualizarConta(
            @PathVariable Long id,
            @Valid @RequestBody ContaFinanceiraRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "atualizar conta financeira");
        return ResponseEntity.ok(fluxoCaixaService.atualizarConta(
                id,
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping({"/parcelamentos", "/parcelas/gerar"})
    public ResponseEntity<List<FluxoCaixaPrevistoItemDTO>> gerarParcelas(
            @Valid @RequestBody GerarParcelasRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "gerar parcelamento no fluxo de caixa");
        return ResponseEntity.status(HttpStatus.CREATED).body(fluxoCaixaService.gerarParcelas(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping({"/receber/{id}/baixar", "/contas-receber/{id}/baixar"})
    public ResponseEntity<MovimentacaoFinanceiraResponseDTO> baixarContaReceber(
            @PathVariable Long id,
            @Valid @RequestBody BaixaTituloFinanceiroRequestDTO body,
            @RequestHeader(value = "Idempotency-Key", required = false) String headerIdemKey,
            HttpServletRequest request) {
        requireManagePermission(request, "baixar conta a receber");
        BaixaTituloFinanceiroRequestDTO merged = mergeIdempotencyKey(body, headerIdemKey);
        return ResponseEntity.ok(fluxoCaixaService.baixarContaReceber(
                id,
                merged,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping({"/pagar/{id}/baixar", "/contas-pagar/{id}/baixar"})
    public ResponseEntity<MovimentacaoFinanceiraResponseDTO> baixarContaPagar(
            @PathVariable Long id,
            @Valid @RequestBody BaixaTituloFinanceiroRequestDTO body,
            @RequestHeader(value = "Idempotency-Key", required = false) String headerIdemKey,
            HttpServletRequest request) {
        requireManagePermission(request, "baixar conta a pagar");
        BaixaTituloFinanceiroRequestDTO merged = mergeIdempotencyKey(body, headerIdemKey);
        return ResponseEntity.ok(fluxoCaixaService.baixarContaPagar(
                id,
                merged,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<List<MovimentacaoFinanceiraResponseDTO>> transferirEntreContas(
            @Valid @RequestBody TransferenciaInternaRequestDTO body,
            @RequestHeader(value = "Idempotency-Key", required = false) String headerIdemKey,
            HttpServletRequest request) {
        requireManagePermission(request, "transferir entre contas proprias");
        TransferenciaInternaRequestDTO merged = (body.idempotencyKey() == null || body.idempotencyKey().isBlank())
                && headerIdemKey != null && !headerIdemKey.isBlank()
                ? new TransferenciaInternaRequestDTO(
                        body.contaOrigemId(),
                        body.contaDestinoId(),
                        body.valor(),
                        body.dataTransferencia(),
                        body.descricao(),
                        body.documentoReferencia(),
                        headerIdemKey)
                : body;
        return ResponseEntity.status(HttpStatus.CREATED).body(fluxoCaixaService.transferirEntreContas(
                merged,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping("/movimentacoes/{id}/estornar")
    public ResponseEntity<MovimentacaoFinanceiraResponseDTO> estornarMovimentacao(
            @PathVariable Long id,
            @Valid @RequestBody EstornoMovimentacaoRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "estornar movimentacao financeira");
        return ResponseEntity.ok(fluxoCaixaService.estornarMovimentacao(
                id,
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @GetMapping("/conciliacao")
    public ResponseEntity<ConciliacaoResumoResponseDTO> consultarConciliacao(
            @RequestParam(required = false) Long contaFinanceiraId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            HttpServletRequest request) {
        requireReadPermission(request);
        StatusConciliacaoFinanceira statusEnum = parseStatusConciliacao(status);
        return ResponseEntity.ok(fluxoCaixaService.consultarConciliacao(
                contaFinanceiraId,
                statusEnum,
                dataInicio,
                dataFim,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)));
    }

    @PostMapping("/conciliacao/importar")
    public ResponseEntity<ConciliacaoResumoResponseDTO> importarExtrato(
            @Valid @RequestBody ImportacaoExtratoRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "importar extrato para conciliacao");
        return ResponseEntity.ok(fluxoCaixaService.importarExtratoConciliacao(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    @PostMapping({"/conciliacao/manual", "/conciliacao/conciliar"})
    public ResponseEntity<ConciliacaoItemResponseDTO> conciliarManualmente(
            @Valid @RequestBody ConciliacaoManualRequestDTO body,
            HttpServletRequest request) {
        requireManagePermission(request, "registrar conciliacao manual");
        return ResponseEntity.ok(fluxoCaixaService.conciliarManualmente(
                body,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request),
                request));
    }

    private void requireReadPermission(HttpServletRequest request) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE,
                    "Financeiro", "consultar fluxo de caixa e conciliacao");
        }
        authorizationService.require(request, ApexPermission.FINANCE_CASHFLOW_VIEW,
                "Financeiro", "consultar fluxo de caixa e conciliacao");
    }

    private void requireManagePermission(HttpServletRequest request, String acao) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE, "Financeiro", acao);
        }
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", acao);
    }

    private BaixaTituloFinanceiroRequestDTO mergeIdempotencyKey(BaixaTituloFinanceiroRequestDTO body, String headerKey) {
        if ((body.idempotencyKey() == null || body.idempotencyKey().isBlank())
                && headerKey != null && !headerKey.isBlank()) {
            return new BaixaTituloFinanceiroRequestDTO(
                    body.valorLiquidado(),
                    body.contaFinanceiraId(),
                    body.dataLiquidacao(),
                    body.valorTaxa(),
                    body.taxaAntecipacao(),
                    body.valorJurosDesconto(),
                    body.antecipacaoRecebivel(),
                    body.observacao(),
                    headerKey);
        }
        return body;
    }

    private StatusConciliacaoFinanceira parseStatusConciliacao(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return StatusConciliacaoFinanceira.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Status de conciliacao invalido: " + raw);
        }
    }
}
