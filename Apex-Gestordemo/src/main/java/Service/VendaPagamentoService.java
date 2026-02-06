package Service;

import Model.*;
import Repository.VendaPagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

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
}
