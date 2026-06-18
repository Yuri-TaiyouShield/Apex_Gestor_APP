package Controller;

import DTO.GrupoProdutoRequestDTO;
import DTO.GrupoProdutoResponseDTO;
import Model.GrupoProduto;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.GrupoProdutoService;
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
@RequestMapping("/api/grupos-produtos")
@CrossOrigin(origins = "http://localhost:4200")
public class GrupoProdutoController {

    @Autowired
    private GrupoProdutoService grupoProdutoService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired
    private AuditTrailService auditTrailService;

    @GetMapping
    public List<GrupoProdutoResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_VIEW, "Produtos", "listar grupos de produto");
        return grupoProdutoService.listarTodos(empresaId(request), filialId(request)).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoProdutoResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_VIEW, "Produtos", "consultar grupo de produto");
        return grupoProdutoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GrupoProdutoResponseDTO> criar(@Valid @RequestBody GrupoProdutoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_CREATE, "Produtos", "criar grupo de produto");
        GrupoProduto salvo = grupoProdutoService.salvar(toEntity(null, body), empresaId(request), filialId(request));
        auditTrailService.registrarPrivacidade("GRUPO_PRODUTO_CRIADO", "GrupoProduto", salvo.getIdGrupoProduto(),
                "Produtos", null, resumo(salvo), request, "Grupo de produto criado.");
        return new ResponseEntity<>(toResponse(salvo), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GrupoProdutoResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody GrupoProdutoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_UPDATE, "Produtos", "atualizar grupo de produto");
        return grupoProdutoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumo(existente);
                    GrupoProduto salvo = grupoProdutoService.salvar(toEntity(id, body), empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade("GRUPO_PRODUTO_ATUALIZADO", "GrupoProduto",
                            salvo.getIdGrupoProduto(), "Produtos", anterior, resumo(salvo), request,
                            "Grupo de produto atualizado.");
                    return ResponseEntity.ok(toResponse(salvo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<GrupoProdutoResponseDTO> desativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_DISABLE, "Produtos", "desativar grupo de produto");
        return grupoProdutoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumo(existente);
                    GrupoProduto salvo = grupoProdutoService.desativar(id, empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade("GRUPO_PRODUTO_DESATIVADO", "GrupoProduto",
                            salvo.getIdGrupoProduto(), "Produtos", anterior, resumo(salvo), request,
                            "Grupo de produto desativado.");
                    return ResponseEntity.ok(toResponse(salvo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private GrupoProduto toEntity(Long id, GrupoProdutoRequestDTO body) {
        GrupoProduto grupo = new GrupoProduto();
        grupo.setIdGrupoProduto(id);
        grupo.setNome(body.nome());
        grupo.setDescricao(body.descricao());
        grupo.setStatus(body.status() == null ? 1 : body.status());
        return grupo;
    }

    private GrupoProdutoResponseDTO toResponse(GrupoProduto grupo) {
        return new GrupoProdutoResponseDTO(
                grupo.getIdGrupoProduto(),
                grupo.getNome(),
                grupo.getDescricao(),
                grupo.getStatus(),
                grupo.getEmpresaId(),
                grupo.getFilialId());
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }

    private String resumo(GrupoProduto grupo) {
        return "nome=" + grupo.getNome() + ";status=" + grupo.getStatus();
    }
}
