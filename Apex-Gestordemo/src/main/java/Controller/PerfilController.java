package Controller;

import DTO.*;
import Model.Perfil;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfis")
public class PerfilController {
    @Autowired private PerfilService service;
    @Autowired private AuthorizationService authorizationService;

    @GetMapping
    public List<PerfilResponseDTO> listar(HttpServletRequest r) {
        require(r, "listar perfis");
        return service.listarTodos().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponseDTO> buscar(@PathVariable Long id, HttpServletRequest r) {
        require(r, "consultar perfil");
        return service.buscarPorId(id).map(p -> ResponseEntity.ok(toResponse(p))).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PerfilResponseDTO> criar(@Valid @RequestBody PerfilRequestDTO b, HttpServletRequest r) {
        require(r, "criar perfil");
        authorizationService.validatePerfilAssignment(r, b.nome());
        validarColisaoNomeCanonico(b.nome(), null);
        Perfil p = new Perfil();
        p.setNome(b.nome().trim());
        p.setStatus(b.status() == null ? 1 : b.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(service.salvar(p)));
    }

    @PutMapping("/{id}")
    public PerfilResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody PerfilRequestDTO b, HttpServletRequest r) {
        require(r, "atualizar perfil");
        Perfil p = service.buscarPorId(id).orElseThrow();
        authorizationService.validatePerfilAssignment(r, p.getNome());
        authorizationService.validatePerfilAssignment(r, b.nome());
        validarColisaoNomeCanonico(b.nome(), id);
        p.setNome(b.nome().trim());
        p.setStatus(b.status() == null ? p.getStatus() : b.status());
        return toResponse(service.salvar(p));
    }

    private void validarColisaoNomeCanonico(String candidateName, Long currentId) {
        String candidateCanonical = AuthorizationService.canonicalRole(candidateName);
        if ("ROLE_PUBLICO".equals(candidateCanonical)) {
            throw new IllegalArgumentException("Nome de perfil invalido.");
        }
        boolean duplicated = service.listarTodos().stream()
                .filter(existing -> currentId == null || !currentId.equals(existing.getIdPerfil()))
                .anyMatch(existing -> candidateCanonical.equals(AuthorizationService.canonicalRole(existing.getNome())));
        if (duplicated) {
            throw new IllegalArgumentException("Ja existe um perfil equivalente cadastrado para este nome.");
        }
    }

    @PatchMapping("/{id}/desativar")
    public PerfilResponseDTO desativar(@PathVariable Long id, HttpServletRequest r) {
        require(r, "desativar perfil");
        Perfil p = service.buscarPorId(id).orElseThrow();
        authorizationService.validatePerfilAssignment(r, p.getNome());
        return toResponse(service.desativar(id));
    }

    @PostMapping("/{perfilId}/menus/{menuId}")
    public PerfilResponseDTO vincular(@PathVariable Long perfilId, @PathVariable Long menuId, HttpServletRequest r) {
        require(r, "vincular menu");
        Perfil p = service.buscarPorId(perfilId).orElseThrow();
        authorizationService.validatePerfilAssignment(r, p.getNome());
        return toResponse(service.vincularMenu(perfilId, menuId));
    }

    @DeleteMapping("/{perfilId}/menus/{menuId}")
    public PerfilResponseDTO desvincular(@PathVariable Long perfilId, @PathVariable Long menuId, HttpServletRequest r) {
        require(r, "desvincular menu");
        Perfil p = service.buscarPorId(perfilId).orElseThrow();
        authorizationService.validatePerfilAssignment(r, p.getNome());
        return toResponse(service.desvincularMenu(perfilId, menuId));
    }

    private void require(HttpServletRequest r, String a) {
        authorizationService.require(r, ApexPermission.ROLE_MANAGE, "Perfis", a);
    }

    private PerfilResponseDTO toResponse(Perfil p) {
        return new PerfilResponseDTO(
                p.getIdPerfil(),
                p.getNome(),
                p.getStatus(),
                p.getMenus() == null ? List.of() : p.getMenus().stream()
                        .map(m -> new PerfilResponseDTO.MenuResumoDTO(m.getIdMenu(), m.getNome(), m.getLink(), m.getIcone(), m.getExibir()))
                        .toList());
    }
}
