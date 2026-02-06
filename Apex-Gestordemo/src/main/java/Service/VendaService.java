package Service;

import Model.*;
import Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VendaService {

    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    public List<Venda> listarTodos() {
        return vendaRepository.findAll();
    }

    public Optional<Venda> buscarPorId(Long id) {
        return vendaRepository.findById(id);
    }

    @Transactional
    public Venda realizarVenda(Venda venda) {
        venda.setDataVenda(LocalDateTime.now());
        venda.setStatus(1);
        BigDecimal valorTotal = BigDecimal.ZERO;

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new RuntimeException("Venda sem itens.");
        }
        Venda vendaSalva = vendaRepository.save(venda);

        for (ProdutoVenda item : venda.getItens()) {
            Produto produto = produtoRepository.findById(item.getProduto().getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

            if (produto.getQuantidadeEstoque() < item.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente");
            }

            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - item.getQuantidade());
            produtoRepository.save(produto);

            // Snapshot do custo e preço
            item.setCustoUnitario(produto.getCusto());
            item.setPrecoUnitario(produto.getValorVenda());

            BigDecimal totalItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            item.setPrecoTotal(totalItem);
            valorTotal = valorTotal.add(totalItem);

            item.setVenda(vendaSalva);
            item.setId(new ProdutoVendaId(produto.getIdProduto(), vendaSalva.getIdVenda()));
        }

        if (venda.getDesconto() != null) {
            valorTotal = valorTotal.subtract(venda.getDesconto());
        }
        vendaSalva.setValorTotal(valorTotal);

        // Pagamentos
        if (venda.getPagamentos() != null) {
            for (VendaPagamento pag : venda.getPagamentos()) {
                pag.setVenda(vendaSalva);
                pag.setId(new VendaPagamentoId(vendaSalva.getIdVenda(), pag.getFormaPagamento().getIdFormaPagamento()));
            }
        }
        return vendaRepository.save(vendaSalva);
    }

    @Transactional
    public Venda cancelarVenda(Long id) {
        Venda venda = vendaRepository.findById(id).orElseThrow(() -> new RuntimeException("Venda não encontrada"));
        if (venda.getStatus() == 0) {
            throw new RuntimeException("Venda já cancelada");
        }

        for (ProdutoVenda item : venda.getItens()) {
            Produto p = item.getProduto();
            p.setQuantidadeEstoque(p.getQuantidadeEstoque() + item.getQuantidade());
            produtoRepository.save(p);
        }
        venda.setStatus(0);
        return vendaRepository.save(venda);
    }
}
