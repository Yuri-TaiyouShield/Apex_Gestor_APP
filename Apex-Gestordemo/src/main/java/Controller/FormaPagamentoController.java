package Controller;

import Model.FormaPagamento;
import Service.FormaPagamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/formaspagamento")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8100", "http://localhost:5173", "app://localhost", "ionic://localhost"})
public class FormaPagamentoController {

    @Autowired
    private FormaPagamentoService formaPagamentoService;

    @GetMapping
    public List<FormaPagamento> listarTodos() {
        return formaPagamentoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormaPagamento> buscarPorId(@PathVariable Long id) {
        return formaPagamentoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FormaPagamento> criar(@Valid @RequestBody FormaPagamento formaPagamento) {
        return new ResponseEntity<>(formaPagamentoService.salvar(formaPagamento), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormaPagamento> atualizar(@PathVariable Long id, @Valid @RequestBody FormaPagamento detalhes) {
        return formaPagamentoService.buscarPorId(id)
                .map(existente -> {
                    detalhes.setIdFormaPagamento(id); // ID Padronizado
                    return ResponseEntity.ok(formaPagamentoService.salvar(detalhes));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<FormaPagamento> desativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(formaPagamentoService.desativar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
