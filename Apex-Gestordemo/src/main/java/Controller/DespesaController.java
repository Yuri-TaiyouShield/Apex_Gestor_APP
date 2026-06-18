package Controller;

import Model.Despesa;
import Model.TipoDespesa;
import Service.AuthorizationService;
import Service.DespesaService;
import Service.FinanceiroService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/despesas")
@CrossOrigin(origins = "http://localhost:4200")
public class DespesaController {

    @Autowired
    private DespesaService despesaService;
    @Autowired
    private FinanceiroService financeiroService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private TenantScopeService tenantScopeService;

    // --- Endpoints de Despesa ---
    @GetMapping
    public List<Despesa> listar(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "listar despesas");
        return despesaService.listarTodos(empresaId(request), filialId(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Despesa> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "consultar despesa");
        return despesaService.buscarPorId(id, empresaId(request), filialId(request))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Despesa> criar(@RequestBody Despesa despesa, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", "criar despesa");
        return new ResponseEntity<>(despesaService.salvar(despesa, empresaId(request), filialId(request)), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Despesa> cancelar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", "cancelar despesa");
        try {
            return ResponseEntity.ok(despesaService.cancelar(id, empresaId(request), filialId(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Endpoints de Tipo de Despesa ---
    @GetMapping("/tipos")
    public List<TipoDespesa> listarTipos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "listar tipos de despesa");
        return financeiroService.listarTiposDespesa();
    }

    @PostMapping("/tipos")
    public ResponseEntity<TipoDespesa> criarTipo(@RequestBody TipoDespesa tipo, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", "criar tipo de despesa");
        return new ResponseEntity<>(financeiroService.salvarTipoDespesa(tipo), HttpStatus.CREATED);
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
