package Repository;

import Model.EstoqueMovimentacao;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstoqueMovimentacaoRepository extends JpaRepository<EstoqueMovimentacao, Long> {

    List<EstoqueMovimentacao> findByProdutoIdProdutoOrderByCriadoEmDesc(Long produtoId);

    List<EstoqueMovimentacao> findByProdutoIdProdutoAndEmpresaIdAndFilialIdOrderByCriadoEmDesc(
            Long produtoId,
            Long empresaId,
            Long filialId);

    List<EstoqueMovimentacao> findByEmpresaIdAndFilialIdOrderByCriadoEmAsc(
            Long empresaId,
            Long filialId);

    List<EstoqueMovimentacao> findByEmpresaIdAndFilialIdAndCriadoEmBetweenOrderByCriadoEmAsc(
            Long empresaId,
            Long filialId,
            LocalDateTime inicio,
            LocalDateTime fim);
}
