package Controller;

import DTO.PerfilResumoDTO;
import DTO.UsuarioRequestDTO;
import DTO.UsuarioResponseDTO;
import Model.Perfil;
import Model.Usuario;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.TenantScopeService;
import Service.UsuarioService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private AuditTrailService auditTrailService;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private TenantScopeService tenantScopeService;

    @GetMapping
    public List<UsuarioResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.USER_VIEW, "Usuarios", "listar usuarios");
        return usuarioService.listarTodos(empresaId(request), filialId(request)).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.USER_VIEW, "Usuarios", "consultar usuario");
        return usuarioService.buscarPorId(id, empresaId(request), filialId(request))
                .map(usuario -> ResponseEntity.ok(toResponse(usuario)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/login/{login}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorLogin(@PathVariable String login, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.USER_VIEW, "Usuarios", "consultar usuario por login");
        return usuarioService.buscarPorLogin(login)
                .filter(usuario -> java.util.Objects.equals(usuario.getEmpresaId(), empresaId(request))
                        && java.util.Objects.equals(usuario.getFilialId(), filialId(request)))
                .map(usuario -> ResponseEntity.ok(toResponse(usuario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.USER_MANAGE, "Usuarios", "criar usuario");
        Usuario salvo = usuarioService.salvar(toEntity(requestBody, null), empresaId(request), filialId(request));
        auditTrailService.registrarPrivacidade(
                "USUARIO_CRIADO",
                "Usuario",
                salvo.getIdUsuario(),
                "Usuarios",
                null,
                resumoUsuario(salvo),
                request,
                "Cadastro de usuario com senha armazenada por hash.");
        return new ResponseEntity<>(toResponse(salvo), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.USER_MANAGE, "Usuarios", "atualizar usuario");
        return usuarioService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumoUsuario(existente);
                    Usuario salvo = usuarioService.salvar(toEntity(requestBody, existente), empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade(
                            "USUARIO_ATUALIZADO",
                            "Usuario",
                            salvo.getIdUsuario(),
                            "Usuarios",
                            anterior,
                            resumoUsuario(salvo),
                            request,
                            "Atualizacao de usuario sem exposicao de senha.");
                    return ResponseEntity.ok(toResponse(salvo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioResponseDTO> desativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.USER_MANAGE, "Usuarios", "desativar usuario");
        try {
            Usuario salvo = usuarioService.desativar(id, empresaId(request), filialId(request));
            auditTrailService.registrarPrivacidade(
                    "USUARIO_DESATIVADO",
                    "Usuario",
                    salvo.getIdUsuario(),
                    "Usuarios",
                    null,
                    resumoUsuario(salvo),
                    request,
                    "Usuario desativado sem remocao fisica.");
            return ResponseEntity.ok(toResponse(salvo));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Usuario toEntity(UsuarioRequestDTO request, Usuario existente) {
        Usuario usuario = existente == null ? new Usuario() : existente;
        usuario.setNome(request.nome());
        usuario.setLogin(request.login());
        usuario.setSenha(request.senha());
        usuario.setDataNascimento(request.dataNascimento());
        if (request.status() != null) {
            usuario.setStatus(request.status());
        }
        usuario.setEmpresaId(request.empresaId());
        usuario.setFilialId(request.filialId());
        if (request.perfil() != null && request.perfil().idPerfil() != null) {
            Perfil perfil = new Perfil();
            perfil.setIdPerfil(request.perfil().idPerfil());
            usuario.setPerfil(perfil);
        }
        return usuario;
    }

    private UsuarioResponseDTO toResponse(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();
        PerfilResumoDTO perfilDto = perfil == null ? null
                : new PerfilResumoDTO(perfil.getIdPerfil(), perfil.getNome(), perfil.getStatus());
        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getLogin(),
                usuario.getDataNascimento(),
                usuario.getStatus(),
                usuario.getEmpresaId(),
                usuario.getFilialId(),
                perfilDto);
    }

    private String resumoUsuario(Usuario usuario) {
        return "id=" + usuario.getIdUsuario()
                + ";login=" + usuario.getLogin()
                + ";perfil=" + (usuario.getPerfil() == null ? null : usuario.getPerfil().getIdPerfil())
                + ";status=" + usuario.getStatus();
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
