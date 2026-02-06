package Service;

import Model.ProdutoVenda;
import Model.ProdutoVendaId;
import Repository.ProdutoVendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

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
}
