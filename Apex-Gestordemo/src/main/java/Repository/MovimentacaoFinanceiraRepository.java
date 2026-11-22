package Repository;

import Model.MovimentacaoFinanceira;
import Model.OrigemMovimentacaoFinanceira;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimentacaoFinanceiraRepository
        extends JpaRepository<MovimentacaoFinanceira, Long>, JpaSpecificationExecutor<MovimentacaoFinanceira> {

    List<MovimentacaoFinanceira> findByEmpresaIdAndFilialIdAndDataMovimentacaoBetweenOrderByDataMovimentacaoDescIdMovimentacaoFinanceiraDesc(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim);

    List<MovimentacaoFinanceira> findByEmpresaIdAndFilialIdAndContaFinanceiraIdAndDataMovimentacaoBetweenOrderByDataMovimentacaoDescIdMovimentacaoFinanceiraDesc(
            Long empresaId, Long filialId, Long contaFinanceiraId, LocalDate inicio, LocalDate fim);

    Optional<MovimentacaoFinanceira> findByIdMovimentacaoFinanceiraAndEmpresaIdAndFilialId(
            Long idMovimentacaoFinanceira, Long empresaId, Long filialId);

    Optional<MovimentacaoFinanceira> findByIdempotencyKeyAndEmpresaIdAndFilialId(
            String idempotencyKey, Long empresaId, Long filialId);

    List<MovimentacaoFinanceira> findByEmpresaIdAndFilialIdAndOrigemTipoAndOrigemIdAndEstornadoFalse(
            Long empresaId, Long filialId, OrigemMovimentacaoFinanceira origemTipo, Long origemId);

    List<MovimentacaoFinanceira> findByEmpresaIdAndFilialIdAndContaFinanceiraIdAndConciliadoFalseAndEstornadoFalseOrderByDataMovimentacaoAsc(
            Long empresaId, Long filialId, Long contaFinanceiraId);
}
