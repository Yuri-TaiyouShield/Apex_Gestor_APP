package Service;

import Model.Produto;
import Model.TipoMovimentacaoEstoque;
import Repository.FornecedorRepository;
import Repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private FornecedorRepository fornecedorRepository;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private EstoqueMovimentacaoService estoqueMovimentacaoService;

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public List<Produto> listarTodos(Long empresaId, Long filialId) {
        return produtoRepository.findByEmpresaIdAndFilialIdOrderByDescricaoAsc(empresaId, filialId);
    }

    public Optional<Produto> buscarPorId(Long id) {
        return produtoRepository.findById(id);
    }

    public Optional<Produto> buscarPorId(Long id, Long empresaId, Long filialId) {
        return produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(id, empresaId, filialId);
    }

    public Produto salvar(Produto produto) {
        return salvar(produto, produto.getEmpresaId(), produto.getFilialId());
    }

    public Produto salvar(Produto produto, Long empresaId, Long filialId) {
        Long empresaAtual = empresaId == null ? 1L : empresaId;
        Long filialAtual = filialId == null ? 1L : filialId;
        if (produto.getDescricao() == null || produto.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descricao do produto e obrigatoria.");
        }
        String codigoBarras = normalize(produto.getCodigoBarras());
        if (codigoBarras == null) {
            throw new IllegalArgumentException("Codigo de barras e obrigatorio para produto vendavel.");
        }
        produto.setCodigoBarras(codigoBarras);
        Optional<Produto> produtoComMesmoCodigo = produtoRepository.findByCodigoBarrasAndEmpresaIdAndFilialId(
                codigoBarras,
                empresaAtual,
                filialAtual);
        if (produtoComMesmoCodigo.isPresent()
                && !produtoComMesmoCodigo.get().getIdProduto().equals(produto.getIdProduto())) {
            throw new IllegalArgumentException("Codigo de barras ja cadastrado neste escopo de empresa/filial.");
        }
        if (produto.getCategoria() == null || produto.getCategoria().getIdCategoria() == null) {
            throw new IllegalArgumentException("Categoria do produto e obrigatoria.");
        }
        if (produto.getFornecedor() == null || produto.getFornecedor().getIdFornecedor() == null) {
            throw new IllegalArgumentException("Fornecedor do produto e obrigatorio.");
        }
        produto.setFornecedor(fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(
                produto.getFornecedor().getIdFornecedor(),
                empresaAtual,
                filialAtual)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor fora do escopo da empresa/filial.")));
        if (produto.getValorVenda() == null) {
            throw new IllegalArgumentException("Valor de venda do produto e obrigatorio.");
        }
        Optional<Produto> anterior = produto.getIdProduto() == null
                ? Optional.empty()
                : buscarPorId(produto.getIdProduto(), empresaAtual, filialAtual);
        if (produto.getIdProduto() != null && anterior.isEmpty()) {
            throw new RuntimeException("Produto nao encontrado no escopo da empresa/filial.");
        }
        int saldoAnterior = anterior.map(Produto::getQuantidadeEstoque).orElse(0);
        if (produto.getCusto() == null) {
            produto.setCusto(BigDecimal.ZERO);
        }
        if (produto.getIdProduto() == null && produto.getStatus() == 0) {
            produto.setStatus(1);
        }
        produto.setEmpresaId(empresaAtual);
        produto.setFilialId(filialAtual);
        Produto salvo = produtoRepository.save(produto);
        auditTrailService.registrarSistema(
                anterior.isPresent() ? "PRODUTO_ATUALIZADO" : "PRODUTO_CRIADO",
                "Produto",
                salvo.getIdProduto(),
                resumoProduto(anterior.orElse(null)),
                resumoProduto(salvo));
        if (saldoAnterior != salvo.getQuantidadeEstoque()) {
            estoqueMovimentacaoService.registrar(
                    salvo,
                    TipoMovimentacaoEstoque.AJUSTE,
                    Math.abs(salvo.getQuantidadeEstoque() - saldoAnterior),
                    saldoAnterior,
                    salvo.getQuantidadeEstoque(),
                    "Produto",
                    salvo.getIdProduto(),
                    "Ajuste de saldo pelo cadastro de produto");
        }
        return salvo;
    }

    public Produto desativar(Long id) {
        Produto p = produtoRepository.findById(id).orElseThrow(() -> new RuntimeException("Nao encontrado"));
        return desativar(p, p.getEmpresaId(), p.getFilialId());
    }

    public Produto desativar(Long id, Long empresaId, Long filialId) {
        Produto p = buscarPorId(id, empresaId, filialId).orElseThrow(() -> new RuntimeException("Nao encontrado"));
        return desativar(p, empresaId, filialId);
    }

    private Produto desativar(Produto p, Long empresaId, Long filialId) {
        String anterior = resumoProduto(p);
        p.setStatus(0);
        p.setEmpresaId(empresaId);
        p.setFilialId(filialId);
        Produto salvo = produtoRepository.save(p);
        auditTrailService.registrarSistema("PRODUTO_DESATIVADO", "Produto", salvo.getIdProduto(), anterior, resumoProduto(salvo));
        return salvo;
    }

    private String resumoProduto(Produto produto) {
        if (produto == null) {
            return null;
        }
        return "descricao=" + produto.getDescricao()
                + ";codigoBarras=" + produto.getCodigoBarras()
                + ";estoque=" + produto.getQuantidadeEstoque()
                + ";valorVenda=" + produto.getValorVenda()
                + ";status=" + produto.getStatus();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
