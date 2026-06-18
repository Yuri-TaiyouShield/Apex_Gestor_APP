package Service;

import Model.*;
import Repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendaService {

    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private EstoqueMovimentacaoService estoqueMovimentacaoService;

    public List<Venda> listarTodos() {
        return vendaRepository.findAll();
    }

    public List<Venda> listarTodos(Long empresaId, Long filialId) {
        return vendaRepository.findByEmpresaIdAndFilialIdOrderByDataVendaDesc(empresaId, filialId);
    }

    public Optional<Venda> buscarPorId(Long id) {
        return vendaRepository.findById(id);
    }

    public Optional<Venda> buscarPorId(Long id, Long empresaId, Long filialId) {
        return vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(id, empresaId, filialId);
    }

    @Transactional
    public Venda realizarVenda(Venda venda) {
        return realizarVenda(venda, venda.getEmpresaId(), venda.getFilialId());
    }

    @Transactional
    public Venda realizarVenda(Venda venda, Long empresaId, Long filialId) {
        Long empresaAtual = empresaId == null ? 1L : empresaId;
        Long filialAtual = filialId == null ? 1L : filialId;
        venda.setDataVenda(LocalDateTime.now());
        venda.setStatus(1);
        venda.setEmpresaId(empresaAtual);
        venda.setFilialId(filialAtual);
        BigDecimal valorTotal = BigDecimal.ZERO;

        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new RuntimeException("Venda sem itens.");
        }
        if (venda.getCliente() == null || venda.getCliente().getIdCliente() == null) {
            throw new RuntimeException("Cliente obrigatorio para venda.");
        }
        venda.setCliente(clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(
                venda.getCliente().getIdCliente(),
                empresaAtual,
                filialAtual)
                .orElseThrow(() -> new RuntimeException("Cliente nao encontrado no escopo da empresa/filial.")));
        Venda vendaSalva = vendaRepository.save(venda);

        for (ProdutoVenda item : venda.getItens()) {
            Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                    item.getProduto().getIdProduto(),
                    empresaAtual,
                    filialAtual)
                    .orElseThrow(() -> new RuntimeException("Produto nao encontrado no escopo da empresa/filial."));

            if (produto.getQuantidadeEstoque() < item.getQuantidade()) {
                throw new RuntimeException("Estoque insuficiente");
            }

            int saldoAnterior = produto.getQuantidadeEstoque();
            int saldoPosterior = saldoAnterior - item.getQuantidade();
            produto.setQuantidadeEstoque(saldoPosterior);
            produtoRepository.save(produto);
            estoqueMovimentacaoService.registrar(
                    produto,
                    TipoMovimentacaoEstoque.SAIDA,
                    item.getQuantidade(),
                    saldoAnterior,
                    saldoPosterior,
                    "Venda",
                    vendaSalva.getIdVenda(),
                    "Saida automatica por venda no PDV");

            item.setCustoUnitario(produto.getCusto());
            item.setPrecoUnitario(produto.getValorVenda());
            item.setProduto(produto);

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

        if (venda.getPagamentos() != null) {
            for (VendaPagamento pag : venda.getPagamentos()) {
                pag.setVenda(vendaSalva);
                pag.setId(new VendaPagamentoId(vendaSalva.getIdVenda(), pag.getFormaPagamento().getIdFormaPagamento()));
            }
        }
        Venda finalizada = vendaRepository.save(vendaSalva);
        auditTrailService.registrarSistema("VENDA_CRIADA", "Venda", finalizada.getIdVenda(), null, resumoVenda(finalizada));
        return finalizada;
    }

    @Transactional
    public Venda cancelarVenda(Long id) {
        Venda venda = vendaRepository.findById(id).orElseThrow(() -> new RuntimeException("Venda nao encontrada"));
        return cancelarVenda(venda, venda.getEmpresaId(), venda.getFilialId());
    }

    @Transactional
    public Venda cancelarVenda(Long id, Long empresaId, Long filialId) {
        Venda venda = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Venda nao encontrada"));
        return cancelarVenda(venda, empresaId, filialId);
    }

    private Venda cancelarVenda(Venda venda, Long empresaId, Long filialId) {
        if (venda.getStatus() == 0) {
            throw new RuntimeException("Venda ja cancelada");
        }

        for (ProdutoVenda item : venda.getItens()) {
            Produto p = item.getProduto();
            int saldoAnterior = p.getQuantidadeEstoque();
            int saldoPosterior = saldoAnterior + item.getQuantidade();
            p.setQuantidadeEstoque(saldoPosterior);
            produtoRepository.save(p);
            estoqueMovimentacaoService.registrar(
                    p,
                    TipoMovimentacaoEstoque.CANCELAMENTO_VENDA,
                    item.getQuantidade(),
                    saldoAnterior,
                    saldoPosterior,
                    "Venda",
                    venda.getIdVenda(),
                    "Estorno automatico por cancelamento de venda");
        }
        String anterior = resumoVenda(venda);
        venda.setStatus(0);
        venda.setEmpresaId(empresaId);
        venda.setFilialId(filialId);
        Venda cancelada = vendaRepository.save(venda);
        auditTrailService.registrarSistema("VENDA_CANCELADA", "Venda", cancelada.getIdVenda(), anterior, resumoVenda(cancelada));
        return cancelada;
    }

    private String resumoVenda(Venda venda) {
        return "status=" + venda.getStatus()
                + ";valorTotal=" + venda.getValorTotal()
                + ";desconto=" + venda.getDesconto()
                + ";empresa=" + venda.getEmpresaId()
                + ";filial=" + venda.getFilialId()
                + ";dataVenda=" + venda.getDataVenda();
    }
}
