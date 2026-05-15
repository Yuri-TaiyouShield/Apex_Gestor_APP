package Service;

import Model.*;
import Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VendaService {

    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Venda> listarTodos() {
        return vendaRepository.findAll();
    }

    public Optional<Venda> buscarPorId(Long id) {
        return vendaRepository.findById(id);
    }

    @Transactional
    public Venda realizarVenda(Venda venda) {
        applyAuthenticatedB2cIdentity(venda);
        venda.setDataVenda(LocalDateTime.now());
        venda.setStatus(1);
        BigDecimal valorTotal = BigDecimal.ZERO;
        List<ProdutoVenda> itens = venda.getItens() == null ? List.of() : new ArrayList<>(venda.getItens());
        List<VendaPagamento> pagamentos = venda.getPagamentos() == null ? null : new ArrayList<>(venda.getPagamentos());

        if (itens == null || itens.isEmpty()) {
            throw new RuntimeException("Venda sem itens.");
        }
        venda.setItens(null);
        venda.setPagamentos(null);
        venda.setValorTotal(BigDecimal.ZERO);
        Venda vendaSalva = vendaRepository.save(venda);

        List<Long> produtoIds = itens.stream()
                .map(item -> item.getProduto().getIdProduto())
                .distinct()
                .toList();
        Map<Long, Produto> produtosPorId = produtoRepository.findByIdProdutoIn(produtoIds).stream()
                .collect(Collectors.toMap(Produto::getIdProduto, Function.identity()));

        for (ProdutoVenda item : itens) {
            Produto produto = produtosPorId.get(item.getProduto().getIdProduto());
            if (produto == null) {
                throw new RuntimeException("Produto não encontrado");
            }

            if (produto.getQuantidadeEstoque() < item.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente");
            }

            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - item.getQuantidade());

            // Snapshot do custo e preço
            BigDecimal custoProduto = produto.getCusto() != null ? produto.getCusto() : BigDecimal.ZERO;
            BigDecimal valorVenda = produto.getValorVenda() != null ? produto.getValorVenda() : BigDecimal.ZERO;
            item.setCustoUnitario(custoProduto);
            item.setPrecoUnitario(valorVenda);

            BigDecimal totalItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            item.setPrecoTotal(totalItem);
            valorTotal = valorTotal.add(totalItem);

            item.setProduto(produto);
            item.setVenda(vendaSalva);
            item.setId(new ProdutoVendaId(produto.getIdProduto(), vendaSalva.getIdVenda()));
        }
        produtoRepository.saveAll(produtosPorId.values());

        if (venda.getDesconto() != null) {
            valorTotal = valorTotal.subtract(venda.getDesconto());
        }
        vendaSalva.setValorTotal(valorTotal);
        vendaSalva.setItens(itens);

        // Pagamentos
        if (pagamentos != null) {
            for (VendaPagamento pag : pagamentos) {
                pag.setVenda(vendaSalva);
                pag.setId(new VendaPagamentoId(vendaSalva.getIdVenda(), pag.getFormaPagamento().getIdFormaPagamento()));
            }
            vendaSalva.setPagamentos(pagamentos);
        }
        return vendaRepository.save(vendaSalva);
    }

    @Transactional
    public Venda cancelarVenda(Long id) {
        Venda venda = vendaRepository.findByIdVenda(id).orElseThrow(() -> new RuntimeException("Venda não encontrada"));
        if (venda.getStatus() == 0) {
            throw new RuntimeException("Venda já cancelada");
        }

        for (ProdutoVenda item : venda.getItens()) {
            Produto p = item.getProduto();
            p.setQuantidadeEstoque(p.getQuantidadeEstoque() + item.getQuantidade());
        }
        venda.setStatus(0);
        return vendaRepository.save(venda);
    }

    private void applyAuthenticatedB2cIdentity(Venda venda) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return;
        }
        boolean isB2cCustomer = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CLIENTE_B2C".equals(authority.getAuthority()));
        if (!isB2cCustomer) {
            return;
        }
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        Cliente cliente = clienteRepository.findByUsuarioLogin(login)
                .orElseThrow(() -> new AccessDeniedException("Cliente B2C nao vinculado ao usuario autenticado."));
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new AccessDeniedException("Usuario B2C nao encontrado."));
        venda.setCliente(cliente);
        venda.setUsuario(usuario);
    }
}
