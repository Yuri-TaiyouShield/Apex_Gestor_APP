package Service;

import Model.*;
import Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class NotaFiscalService {

    @Autowired
    private NotaFiscalEntradaRepository nfRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    @Transactional
    public NotaFiscalEntrada darEntrada(NotaFiscalEntrada nf) {
        nf.setDataEntrada(LocalDateTime.now());
        BigDecimal totalNF = BigDecimal.ZERO;

        for (ItemNotaFiscal item : nf.getItens()) {
            Produto produto = produtoRepository.findById(item.getProduto().getIdProduto())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

            // Cálculo Custo Médio
            int qtdAtual = produto.getQuantidadeEstoque();
            BigDecimal custoAtual = produto.getCusto();
            int qtdNova = item.getQuantidade();
            BigDecimal custoNovo = item.getValorCustoUnitario();
            BigDecimal totalEstoque = custoAtual.multiply(new BigDecimal(qtdAtual));
            BigDecimal totalEntrada = custoNovo.multiply(new BigDecimal(qtdNova));

            if ((qtdAtual + qtdNova) > 0) {
                produto.setCusto(totalEstoque.add(totalEntrada).divide(new BigDecimal(qtdAtual + qtdNova), 2, RoundingMode.HALF_UP));
            } else {
                produto.setCusto(custoNovo);
            }
            produto.setQuantidadeEstoque(qtdAtual + qtdNova);
            produtoRepository.save(produto);

            item.setNotaFiscal(nf);
            item.setProduto(produto);
            totalNF = totalNF.add(totalEntrada);
        }
        nf.setValorTotal(totalNF);
        return nfRepository.save(nf);
    }
}
