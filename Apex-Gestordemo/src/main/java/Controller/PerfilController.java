package Controller;

import Model.Perfil;
import Service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/perfis")
@CrossOrigin(origins = "http://localhost:4200")
public class PerfilController {

    @Autowired
    private PerfilService perfilService;

    @GetMapping
    public List<Perfil> listarTodos() {
        return perfilService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Perfil> buscarPorId(@PathVariable Long id) {
        return perfilService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Perfil> criar(@Valid @RequestBody Perfil perfil) {
        return new ResponseEntity<>(perfilService.salvar(perfil), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Perfil> atualizar(@PathVariable Long id, @Valid @RequestBody Perfil perfilDetalhes) {
        return perfilService.buscarPorId(id)
                .map(existente -> {
                    perfilDetalhes.setIdPerfil(id); // ID Padronizado
                    return ResponseEntity.ok(perfilService.salvar(perfilDetalhes));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Perfil> desativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(perfilService.desativar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
