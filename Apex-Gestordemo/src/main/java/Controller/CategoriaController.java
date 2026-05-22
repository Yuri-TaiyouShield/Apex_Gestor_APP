package Controller;

import Model.Categoria;
import Service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8100", "http://localhost:5173", "app://localhost", "ionic://localhost"})
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public List<Categoria> listarTodos() {
        return categoriaService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscarPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Categoria> criar(@Valid @RequestBody Categoria categoria) {
        return new ResponseEntity<>(categoriaService.salvar(categoria), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> atualizar(@PathVariable Long id, @Valid @RequestBody Categoria categoriaDetalhes) {
        return categoriaService.buscarPorId(id)
                .map(existente -> {
                    categoriaDetalhes.setIdCategoria(id); // ID Padronizado
                    return ResponseEntity.ok(categoriaService.salvar(categoriaDetalhes));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Categoria> desativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(categoriaService.desativar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
