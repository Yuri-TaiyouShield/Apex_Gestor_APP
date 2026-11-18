package Service;

import Model.Categoria;
import Repository.CategoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private TenantScopeService tenantScopeService;

    public List<Categoria> listarTodos() {
        HttpServletRequest req = currentRequest();
        return listarTodos(tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public List<Categoria> listarTodos(Long empresaId, Long filialId) {
        return categoriaRepository.findVisibleToTenant(empresaId, filialId);
    }

    public Optional<Categoria> buscarPorId(Long id) {
        HttpServletRequest req = currentRequest();
        return buscarPorId(id, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public Optional<Categoria> buscarPorId(Long id, Long empresaId, Long filialId) {
        return categoriaRepository.findVisibleById(id, empresaId, filialId);
    }

    public Categoria salvar(Categoria categoria) {
        HttpServletRequest req = currentRequest();
        return salvar(categoria, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public Categoria salvar(Categoria categoria, Long empresaId, Long filialId) {
        if (categoria.getDescricao() == null || categoria.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descricao da categoria e obrigatoria.");
        }
        if (categoria.getIdCategoria() != null) {
            Categoria existente = categoriaRepository
                    .findVisibleById(categoria.getIdCategoria(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Categoria nao encontrada no escopo da empresa/filial."));
            if (existente.getEmpresaId() == null) {
                throw new AccessDeniedException("Registros globais do catalogo nao podem ser alterados pelo tenant.");
            }
            if (categoria.getPlanoContaId() == null) {
                categoria.setPlanoContaId(existente.getPlanoContaId());
            }
            if (categoria.getCentroCustoId() == null) {
                categoria.setCentroCustoId(existente.getCentroCustoId());
            }
        }
        if (categoria.getStatus() == 0) {
            categoria.setStatus(1);
        }
        categoria.setEmpresaId(empresaId);
        categoria.setFilialId(filialId);
        return categoriaRepository.save(categoria);
    }

    public Categoria desativar(Long id) {
        HttpServletRequest req = currentRequest();
        return desativar(id, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public Categoria desativar(Long id, Long empresaId, Long filialId) {
        Categoria categoria = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Categoria nao encontrada no escopo da empresa/filial."));
        if (categoria.getEmpresaId() == null) {
            throw new AccessDeniedException("Registros globais do catalogo nao podem ser alterados pelo tenant.");
        }
        categoria.setStatus(0);
        categoria.setEmpresaId(empresaId);
        categoria.setFilialId(filialId);
        return categoriaRepository.save(categoria);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
