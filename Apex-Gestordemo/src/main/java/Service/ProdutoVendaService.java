package Service;

import Model.ProdutoVenda;
import Model.ProdutoVendaId;
import Repository.ProdutoVendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoVendaService {

    @Autowired
    private ProdutoVendaRepository itemVendaRepository;

    public List<ProdutoVenda> listarTodos() {
        return itemVendaRepository.findAll();
    }

    public Optional<ProdutoVenda> buscarPorId(ProdutoVendaId id) {
        return itemVendaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ProdutoVenda> listarTodos(Long empresaId, Long filialId) {
        List<ProdutoVenda> itens = itemVendaRepository.findByVendaEmpresaIdAndVendaFilialId(empresaId, filialId);
        itens.forEach(this::initialize);
        return itens;
    }

    @Transactional(readOnly = true)
    public Optional<ProdutoVenda> buscarPorId(ProdutoVendaId id, Long empresaId, Long filialId) {
        Optional<ProdutoVenda> item = itemVendaRepository.findByIdAndVendaEmpresaIdAndVendaFilialId(id, empresaId, filialId);
        item.ifPresent(this::initialize);
        return item;
    }

    private void initialize(ProdutoVenda item) {
        org.hibernate.Hibernate.initialize(item.getProduto());
        org.hibernate.Hibernate.initialize(item.getVenda());
    }
}
