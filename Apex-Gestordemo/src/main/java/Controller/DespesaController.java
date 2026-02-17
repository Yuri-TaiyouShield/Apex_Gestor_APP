package Controller;

import Model.Despesa;
import Model.TipoDespesa;
import Service.DespesaService;
import Service.FinanceiroService;
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

    // --- Endpoints de Despesa ---
    @GetMapping
    public List<Despesa> listar() {
        return despesaService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Despesa> buscarPorId(@PathVariable Long id) {
        return despesaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Despesa> criar(@RequestBody Despesa despesa) {
        return new ResponseEntity<>(despesaService.salvar(despesa), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Despesa> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(despesaService.cancelar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Endpoints de Tipo de Despesa ---
    @GetMapping("/tipos")
    public List<TipoDespesa> listarTipos() {
        return financeiroService.listarTiposDespesa();
    }

    @PostMapping("/tipos")
    public ResponseEntity<TipoDespesa> criarTipo(@RequestBody TipoDespesa tipo) {
        return new ResponseEntity<>(financeiroService.salvarTipoDespesa(tipo), HttpStatus.CREATED);
    }
}
