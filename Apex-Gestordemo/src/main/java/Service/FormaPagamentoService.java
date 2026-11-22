package Service;

import Model.FormaPagamento;
import Repository.FormaPagamentoRepository;
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
public class FormaPagamentoService {

    @Autowired
    private FormaPagamentoRepository formaPagamentoRepository;
    @Autowired
    private TenantScopeService tenantScopeService;

    public List<FormaPagamento> listarTodos() {
        HttpServletRequest req = currentRequest();
        return listarTodos(tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public List<FormaPagamento> listarTodos(Long empresaId, Long filialId) {
        return formaPagamentoRepository.findVisibleToTenant(empresaId, filialId);
    }

    public Optional<FormaPagamento> buscarPorId(Long id) {
        HttpServletRequest req = currentRequest();
        return buscarPorId(id, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public Optional<FormaPagamento> buscarPorId(Long id, Long empresaId, Long filialId) {
        return formaPagamentoRepository.findVisibleById(id, empresaId, filialId);
    }

    public FormaPagamento salvar(FormaPagamento formaPagamento) {
        HttpServletRequest req = currentRequest();
        return salvar(formaPagamento, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public FormaPagamento salvar(FormaPagamento formaPagamento, Long empresaId, Long filialId) {
        if (formaPagamento.getNome() == null || formaPagamento.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da forma de pagamento e obrigatorio.");
        }
        if (formaPagamento.getDescricao() == null || formaPagamento.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descricao da forma de pagamento e obrigatoria.");
        }
        if (formaPagamento.getIdFormaPagamento() != null) {
            FormaPagamento existente = formaPagamentoRepository
                    .findVisibleById(formaPagamento.getIdFormaPagamento(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Forma de pagamento nao encontrada no escopo da empresa/filial."));
            if (existente.getEmpresaId() == null) {
                throw new AccessDeniedException("Registros globais do catalogo nao podem ser alterados pelo tenant.");
            }
        }
        if (formaPagamento.getStatus() == 0) {
            formaPagamento.setStatus(1);
        }
        formaPagamento.setEmpresaId(empresaId);
        formaPagamento.setFilialId(filialId);
        return formaPagamentoRepository.save(formaPagamento);
    }

    public FormaPagamento desativar(Long id) {
        HttpServletRequest req = currentRequest();
        return desativar(id, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    public FormaPagamento desativar(Long id, Long empresaId, Long filialId) {
        FormaPagamento formaPagamento = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Forma de pagamento nao encontrada no escopo da empresa/filial."));
        if (formaPagamento.getEmpresaId() == null) {
            throw new AccessDeniedException("Registros globais do catalogo nao podem ser alterados pelo tenant.");
        }
        formaPagamento.setStatus(0);
        formaPagamento.setEmpresaId(empresaId);
        formaPagamento.setFilialId(filialId);
        return formaPagamentoRepository.save(formaPagamento);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
