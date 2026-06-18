package Service;

import Model.EstoqueMovimentacao;
import Model.Produto;
import Model.TipoMovimentacaoEstoque;
import Repository.EstoqueMovimentacaoRepository;
import Repository.ProdutoRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueMovimentacaoService {

    @Autowired
    private EstoqueMovimentacaoRepository estoqueMovimentacaoRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private AuditTrailService auditTrailService;

    public List<EstoqueMovimentacao> listarPorProduto(Long produtoId) {
        return estoqueMovimentacaoRepository.findByProdutoIdProdutoOrderByCriadoEmDesc(produtoId);
    }

    public List<EstoqueMovimentacao> listarPorProduto(Long produtoId, Long empresaId, Long filialId) {
        return estoqueMovimentacaoRepository.findByProdutoIdProdutoAndEmpresaIdAndFilialIdOrderByCriadoEmDesc(
                produtoId,
                empresaId,
                filialId);
    }

    @Transactional
    public EstoqueMovimentacao aplicar(
            Long produtoId,
            TipoMovimentacaoEstoque tipo,
            int quantidade,
            Integer saldoContado,
            String referenciaTipo,
            Long referenciaId,
            String motivo,
            Long empresaId,
            Long filialId,
            Long usuarioId) {
        Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(produtoId, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Produto nao encontrado no escopo da empresa/filial"));
        int saldoAnterior = produto.getQuantidadeEstoque();
        int saldoPosterior = calcularSaldoPosterior(tipo, quantidade, saldoContado, saldoAnterior);

        if (saldoPosterior < 0) {
            throw new RuntimeException("Movimentacao deixaria o estoque negativo");
        }

        produto.setQuantidadeEstoque(saldoPosterior);
        produtoRepository.save(produto);

        EstoqueMovimentacao movimentacao = registrar(
                produto,
                tipo,
                Math.abs(saldoPosterior - saldoAnterior),
                saldoAnterior,
                saldoPosterior,
                referenciaTipo,
                referenciaId,
                motivo);
        movimentacao.setEmpresaId(empresaId);
        movimentacao.setFilialId(filialId);
        movimentacao.setUsuarioId(usuarioId);
        EstoqueMovimentacao salva = estoqueMovimentacaoRepository.save(movimentacao);

        auditTrailService.registrarSistema(
                "ESTOQUE_" + tipo.name(),
                "Produto",
                produto.getIdProduto(),
                "saldo=" + saldoAnterior,
                "saldo=" + saldoPosterior + ";movimentacao=" + salva.getIdMovimentacao());
        return salva;
    }

    public EstoqueMovimentacao registrar(
            Produto produto,
            TipoMovimentacaoEstoque tipo,
            int quantidade,
            int saldoAnterior,
            int saldoPosterior,
            String referenciaTipo,
            Long referenciaId,
            String motivo) {
        EstoqueMovimentacao movimentacao = new EstoqueMovimentacao();
        movimentacao.setProduto(produto);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setSaldoAnterior(saldoAnterior);
        movimentacao.setSaldoPosterior(saldoPosterior);
        movimentacao.setReferenciaTipo(referenciaTipo);
        movimentacao.setReferenciaId(referenciaId);
        movimentacao.setMotivo(motivo);
        movimentacao.setEmpresaId(produto.getEmpresaId());
        movimentacao.setFilialId(produto.getFilialId());
        return estoqueMovimentacaoRepository.save(movimentacao);
    }

    private int calcularSaldoPosterior(TipoMovimentacaoEstoque tipo, int quantidade, Integer saldoContado, int saldoAnterior) {
        if (tipo == TipoMovimentacaoEstoque.ENTRADA || tipo == TipoMovimentacaoEstoque.CANCELAMENTO_VENDA) {
            return saldoAnterior + quantidade;
        }
        if (tipo == TipoMovimentacaoEstoque.SAIDA) {
            return saldoAnterior - quantidade;
        }
        if (tipo == TipoMovimentacaoEstoque.AJUSTE || tipo == TipoMovimentacaoEstoque.INVENTARIO) {
            if (saldoContado == null) {
                throw new RuntimeException("Saldo contado e obrigatorio para ajuste ou inventario");
            }
            return saldoContado;
        }
        if (tipo == TipoMovimentacaoEstoque.TRANSFERENCIA) {
            return saldoAnterior - quantidade;
        }
        return saldoAnterior;
    }
}
