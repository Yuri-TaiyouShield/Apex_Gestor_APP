package Controller;

import Model.Fornecedor;
import Service.FornecedorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
@CrossOrigin(origins = "http://localhost:4200")
public class FornecedorController {

    @Autowired private FornecedorService fornecedorService;

    @GetMapping
    public List<Fornecedor> listarTodos() {
        return fornecedorService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Fornecedor> buscarPorId(@PathVariable Long id) {
        return fornecedorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Fornecedor> criar(@Valid @RequestBody Fornecedor fornecedor) {
        return new ResponseEntity<>(fornecedorService.salvar(fornecedor), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Fornecedor> atualizar(@PathVariable Long id, @Valid @RequestBody Fornecedor fornecedorDetalhes) {
        return fornecedorService.buscarPorId(id)
                .map(existente -> {
                    fornecedorDetalhes.setIdFornecedor(id); // ID Padronizado
                    return ResponseEntity.ok(fornecedorService.salvar(fornecedorDetalhes));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Fornecedor> desativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(fornecedorService.desativar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}