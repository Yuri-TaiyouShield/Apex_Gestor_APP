package Controller;

import DTO.CategoriaRequestDTO;
import DTO.CategoriaResponseDTO;
import Model.Categoria;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.CategoriaService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(originPatterns = {"http://localhost:*", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired
    private AuditTrailService auditTrailService;

    @GetMapping
    public List<CategoriaResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_VIEW, "Produtos", "listar categorias");
        return categoriaService.listarTodos(empresaId(request), filialId(request)).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_VIEW, "Produtos", "consultar categoria");
        return categoriaService.buscarPorId(id, empresaId(request), filialId(request))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> criar(@Valid @RequestBody CategoriaRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_CREATE, "Produtos", "criar categoria");
        Categoria salva = categoriaService.salvar(toEntity(null, body), empresaId(request), filialId(request));
        auditTrailService.registrarPrivacidade("CATEGORIA_CRIADA", "Categoria", salva.getIdCategoria(), "Produtos",
                null, resumo(salva), request, "Cadastro auxiliar de produto criado.");
        return new ResponseEntity<>(toResponse(salva), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody CategoriaRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_UPDATE, "Produtos", "atualizar categoria");
        return categoriaService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumo(existente);
                    Categoria salva = categoriaService.salvar(toEntity(id, body), empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade("CATEGORIA_ATUALIZADA", "Categoria", salva.getIdCategoria(),
                            "Produtos", anterior, resumo(salva), request, "Cadastro auxiliar de produto atualizado.");
                    return ResponseEntity.ok(toResponse(salva));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<CategoriaResponseDTO> desativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_DISABLE, "Produtos", "desativar categoria");
        return categoriaService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumo(existente);
                    Categoria salva = categoriaService.desativar(id, empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade("CATEGORIA_DESATIVADA", "Categoria", salva.getIdCategoria(),
                            "Produtos", anterior, resumo(salva), request, "Cadastro auxiliar de produto desativado.");
                    return ResponseEntity.ok(toResponse(salva));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Categoria toEntity(Long id, CategoriaRequestDTO body) {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(id);
        categoria.setDescricao(body.descricao());
        categoria.setStatus(body.status() == null ? 1 : body.status());
        return categoria;
    }

    private CategoriaResponseDTO toResponse(Categoria categoria) {
        return new CategoriaResponseDTO(
                categoria.getIdCategoria(),
                categoria.getDescricao(),
                categoria.getStatus(),
                categoria.getEmpresaId(),
                categoria.getFilialId());
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }

    private String resumo(Categoria categoria) {
        return "descricao=" + categoria.getDescricao() + ";status=" + categoria.getStatus();
    }
}
