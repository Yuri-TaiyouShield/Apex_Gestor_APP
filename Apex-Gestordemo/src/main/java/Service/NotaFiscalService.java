package Service;

import Model.*;
import Repository.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotaFiscalService {
    @Autowired private NotaFiscalEntradaRepository nfRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private EstoqueMovimentacaoService estoqueMovimentacaoService;
    @Autowired private AuditTrailService auditTrailService;

    @Transactional
    public NotaFiscalEntrada darEntrada(NotaFiscalEntrada nf, Long empresaId, Long filialId, HttpServletRequest request) {
        nf.setEmpresaId(empresaId);
        nf.setFilialId(filialId);
        nf.setFornecedor(fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(
                nf.getFornecedor().getIdFornecedor(), empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor nao encontrado no tenant.")));
        nf.setDataEntrada(LocalDateTime.now());
        BigDecimal totalNF = BigDecimal.ZERO;
        for (ItemNotaFiscal item : nf.getItens()) {
            Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                    item.getProduto().getIdProduto(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado no tenant."));
            int qtdAtual = produto.getQuantidadeEstoque();
            int qtdNova = item.getQuantidade();
            BigDecimal totalEstoque = produto.getCusto().multiply(BigDecimal.valueOf(qtdAtual));
            BigDecimal totalEntrada = item.getValorCustoUnitario().multiply(BigDecimal.valueOf(qtdNova));
            produto.setCusto(qtdAtual + qtdNova > 0
                    ? totalEstoque.add(totalEntrada).divide(BigDecimal.valueOf(qtdAtual + qtdNova), 2, RoundingMode.HALF_UP)
                    : item.getValorCustoUnitario());
            produto.setQuantidadeEstoque(qtdAtual + qtdNova);
            produtoRepository.save(produto);
            estoqueMovimentacaoService.registrar(produto, TipoMovimentacaoEstoque.ENTRADA, qtdNova,
                    qtdAtual, qtdAtual + qtdNova, "NotaFiscalEntrada", null, "Entrada por nota fiscal");
            item.setNotaFiscal(nf);
            item.setProduto(produto);
            totalNF = totalNF.add(totalEntrada);
        }
        nf.setValorTotal(totalNF);
        NotaFiscalEntrada salva = nfRepository.save(nf);
        auditTrailService.registrarPrivacidade("NOTA_FISCAL_ENTRADA", "NotaFiscalEntrada", salva.getIdNfEntrada(),
                "Estoque", null, "numero=" + salva.getNumero() + ";valor=" + salva.getValorTotal(), request,
                "Entrada de estoque por nota fiscal no tenant ativo.");
        return salva;
    }

    public NotaFiscalEntrada darEntrada(NotaFiscalEntrada nf) {
        return darEntrada(nf, nf.getEmpresaId(), nf.getFilialId(), null);
    }

    @Transactional(readOnly = true)
    public List<NotaFiscalEntrada> listar(Long empresaId, Long filialId) {
        List<NotaFiscalEntrada> notas = nfRepository.findByEmpresaIdAndFilialIdOrderByDataEntradaDesc(empresaId, filialId);
        notas.forEach(n -> {
            org.hibernate.Hibernate.initialize(n.getFornecedor());
            org.hibernate.Hibernate.initialize(n.getItens());
            n.getItens().forEach(i -> org.hibernate.Hibernate.initialize(i.getProduto()));
        });
        return notas;
    }
}
