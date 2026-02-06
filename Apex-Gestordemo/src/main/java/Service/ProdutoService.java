package Service;

import Model.Produto;
import Repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarPorId(Long id) {
        return produtoRepository.findById(id);
    }

    public Produto salvar(Produto produto) {
        if (produto.getCusto() == null) {
            produto.setCusto(BigDecimal.ZERO);
        }
        return produtoRepository.save(produto);
    }

    public Produto desativar(Long id) {
        Produto p = produtoRepository.findById(id).orElseThrow(() -> new RuntimeException("Não encontrado"));
        p.setStatus(0);
        return produtoRepository.save(p);
    }
}
