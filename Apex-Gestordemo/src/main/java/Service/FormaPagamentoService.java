package Service;

import Model.FormaPagamento;
import Repository.FormaPagamentoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FormaPagamentoService {

    @Autowired
    private FormaPagamentoRepository formaPagamentoRepository;

    public List<FormaPagamento> listarTodos() {
        return formaPagamentoRepository.findAll();
    }

    public List<FormaPagamento> listarTodos(Long empresaId, Long filialId) {
        return formaPagamentoRepository.findVisibleToTenant(empresaId, filialId);
    }

    public Optional<FormaPagamento> buscarPorId(Long id) {
        return formaPagamentoRepository.findById(id);
    }

    public Optional<FormaPagamento> buscarPorId(Long id, Long empresaId, Long filialId) {
        return formaPagamentoRepository.findVisibleById(id, empresaId, filialId);
    }

    public FormaPagamento salvar(FormaPagamento formaPagamento) {
        return formaPagamentoRepository.save(formaPagamento);
    }

    public FormaPagamento salvar(FormaPagamento formaPagamento, Long empresaId, Long filialId) {
        if (formaPagamento.getNome() == null || formaPagamento.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da forma de pagamento e obrigatorio.");
        }
        if (formaPagamento.getDescricao() == null || formaPagamento.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descricao da forma de pagamento e obrigatoria.");
        }
        if (formaPagamento.getStatus() == 0) {
            formaPagamento.setStatus(1);
        }
        formaPagamento.setEmpresaId(empresaId);
        formaPagamento.setFilialId(filialId);
        return formaPagamentoRepository.save(formaPagamento);
    }

    public FormaPagamento desativar(Long id) {
        FormaPagamento formaPagamento = formaPagamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Forma de pagamento nao encontrada"));
        formaPagamento.setStatus(0);
        return formaPagamentoRepository.save(formaPagamento);
    }

    public FormaPagamento desativar(Long id, Long empresaId, Long filialId) {
        FormaPagamento formaPagamento = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Forma de pagamento nao encontrada no escopo da empresa/filial."));
        formaPagamento.setStatus(0);
        formaPagamento.setEmpresaId(empresaId);
        formaPagamento.setFilialId(filialId);
        return formaPagamentoRepository.save(formaPagamento);
    }
}
