package Repository;

import Model.ConciliacaoFinanceiraItem;
import Model.StatusConciliacaoFinanceira;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ConciliacaoFinanceiraItemRepository
        extends JpaRepository<ConciliacaoFinanceiraItem, Long>, JpaSpecificationExecutor<ConciliacaoFinanceiraItem> {

    List<ConciliacaoFinanceiraItem> findByEmpresaIdAndFilialIdOrderByDataExtratoDescIdConciliacaoItemDesc(
            Long empresaId, Long filialId);

    List<ConciliacaoFinanceiraItem> findByEmpresaIdAndFilialIdAndContaFinanceiraIdOrderByDataExtratoDescIdConciliacaoItemDesc(
            Long empresaId, Long filialId, Long contaFinanceiraId);

    List<ConciliacaoFinanceiraItem> findByEmpresaIdAndFilialIdAndDataExtratoBetweenOrderByDataExtratoDescIdConciliacaoItemDesc(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim);

    List<ConciliacaoFinanceiraItem> findByEmpresaIdAndFilialIdAndStatusConciliacaoOrderByDataExtratoDesc(
            Long empresaId, Long filialId, StatusConciliacaoFinanceira status);

    Optional<ConciliacaoFinanceiraItem> findByIdConciliacaoItemAndEmpresaIdAndFilialId(
            Long idConciliacaoItem, Long empresaId, Long filialId);

    List<ConciliacaoFinanceiraItem> findByEmpresaIdAndFilialIdAndContaFinanceiraIdAndDataExtratoAndValorExtratoAndNaturezaExtrato(
            Long empresaId, Long filialId, Long contaFinanceiraId, LocalDate dataExtrato, java.math.BigDecimal valorExtrato, String naturezaExtrato);
}
