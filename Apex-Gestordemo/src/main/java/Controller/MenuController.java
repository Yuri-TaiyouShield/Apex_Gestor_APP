package Controller;

import DTO.*;
import Model.Menu;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus")
public class MenuController {
    @Autowired private MenuService menuService;
    @Autowired private AuthorizationService authorizationService;

    @GetMapping public List<MenuResponseDTO> listarTodos(HttpServletRequest request) {
        require(request, "listar menus");
        return menuService.listarTodos().stream().map(this::toResponse).toList();
    }
    @GetMapping("/{id}") public ResponseEntity<MenuResponseDTO> buscar(@PathVariable Long id, HttpServletRequest request) {
        require(request, "consultar menu");
        return menuService.buscarPorId(id).map(m -> ResponseEntity.ok(toResponse(m))).orElse(ResponseEntity.notFound().build());
    }
    @PostMapping public ResponseEntity<MenuResponseDTO> criar(@Valid @RequestBody MenuRequestDTO body, HttpServletRequest request) {
        require(request, "criar menu");
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(menuService.salvar(from(body, null))));
    }
    @PutMapping("/{id}") public MenuResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody MenuRequestDTO body, HttpServletRequest request) {
        require(request, "atualizar menu");
        Menu atual = menuService.buscarPorId(id).orElseThrow();
        atual.setNome(body.nome()); atual.setLink(body.link()); atual.setIcone(body.icone());
        atual.setExibir(body.exibir() == null ? atual.getExibir() : body.exibir());
        return toResponse(menuService.salvar(atual));
    }
    @PatchMapping("/{id}/desativar") public MenuResponseDTO desativar(@PathVariable Long id, HttpServletRequest request) {
        require(request, "desativar menu"); return toResponse(menuService.desativar(id));
    }
    private void require(HttpServletRequest r, String a) { authorizationService.require(r, ApexPermission.ROLE_MANAGE, "Menus", a); }
    private Menu from(MenuRequestDTO b, Long id) { Menu m = new Menu(); m.setIdMenu(id); m.setNome(b.nome()); m.setLink(b.link()); m.setIcone(b.icone()); m.setExibir(b.exibir() == null ? 1 : b.exibir()); return m; }
    private MenuResponseDTO toResponse(Menu m) { return new MenuResponseDTO(m.getIdMenu(), m.getNome(), m.getLink(), m.getIcone(), m.getExibir(),
            m.getPerfis() == null ? List.of() : m.getPerfis().stream().map(p -> new PerfilResumoDTO(p.getIdPerfil(), p.getNome(), p.getStatus())).toList()); }
}
