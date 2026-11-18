package Service;

import Model.*;
import Repository.VendaPagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendaPagamentoService {

    @Autowired
    private VendaPagamentoRepository pagamentoVendaRepository;

    public List<VendaPagamento> listarTodos() {
        return pagamentoVendaRepository.findAll();
    }

    public Optional<VendaPagamento> buscarPorId(VendaPagamentoId id) {
        return pagamentoVendaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<VendaPagamento> listarTodos(Long empresaId, Long filialId) {
        List<VendaPagamento> pagamentos = pagamentoVendaRepository.findByVendaEmpresaIdAndVendaFilialId(empresaId, filialId);
        pagamentos.forEach(this::initialize);
        return pagamentos;
    }

    @Transactional(readOnly = true)
    public Optional<VendaPagamento> buscarPorId(VendaPagamentoId id, Long empresaId, Long filialId) {
        Optional<VendaPagamento> pagamento = pagamentoVendaRepository.findByIdAndVendaEmpresaIdAndVendaFilialId(id, empresaId, filialId);
        pagamento.ifPresent(this::initialize);
        return pagamento;
    }

    private void initialize(VendaPagamento pagamento) {
        org.hibernate.Hibernate.initialize(pagamento.getVenda());
        org.hibernate.Hibernate.initialize(pagamento.getFormaPagamento());
    }
}
