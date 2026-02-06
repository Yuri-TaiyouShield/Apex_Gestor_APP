package Controller;

import Model.Menu;
import Service.MenuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/menus")
@CrossOrigin(origins = "http://localhost:4200")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping
    public List<Menu> listarTodos() {
        return menuService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Menu> buscarPorId(@PathVariable Long id) {
        return menuService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Menu> criar(@Valid @RequestBody Menu menu) {
        return new ResponseEntity<>(menuService.salvar(menu), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Menu> atualizar(@PathVariable Long id, @Valid @RequestBody Menu menuDetalhes) {
        return menuService.buscarPorId(id)
                .map(existente -> {
                    menuDetalhes.setIdMenu(id); // ID Padronizado
                    return ResponseEntity.ok(menuService.salvar(menuDetalhes));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Menu> desativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(menuService.desativar(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
