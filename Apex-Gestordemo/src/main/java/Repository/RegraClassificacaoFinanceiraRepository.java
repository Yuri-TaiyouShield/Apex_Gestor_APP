package Repository;

import Model.OrigemClassificacaoFinanceira;
import Model.RegraClassificacaoFinanceira;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RegraClassificacaoFinanceiraRepository
        extends JpaRepository<RegraClassificacaoFinanceira, Long>,
                JpaSpecificationExecutor<RegraClassificacaoFinanceira> {

    List<RegraClassificacaoFinanceira> findByEmpresaIdAndFilialIdOrderByPrioridadeAscIdRegraClassificacaoAsc(
            Long empresaId, Long filialId);

    List<RegraClassificacaoFinanceira> findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPrioridadeAscIdRegraClassificacaoAsc(
            Long empresaId, Long filialId);

    List<RegraClassificacaoFinanceira> findByEmpresaIdAndFilialIdAndOrigemTipoAndAtivoTrueOrderByPrioridadeAscIdRegraClassificacaoAsc(
            Long empresaId, Long filialId, OrigemClassificacaoFinanceira origemTipo);

    Optional<RegraClassificacaoFinanceira> findByIdRegraClassificacaoAndEmpresaIdAndFilialId(
            Long idRegraClassificacao, Long empresaId, Long filialId);

    boolean existsByPlanoContaIdAndEmpresaIdAndFilialId(
            Long planoContaId, Long empresaId, Long filialId);

    boolean existsByCentroCustoIdAndEmpresaIdAndFilialId(
            Long centroCustoId, Long empresaId, Long filialId);
}
