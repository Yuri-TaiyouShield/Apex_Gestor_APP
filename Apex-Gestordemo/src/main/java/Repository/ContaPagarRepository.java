package Repository;

import Model.ContaPagar;
import Model.FinanceiroStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContaPagarRepository
        extends JpaRepository<ContaPagar, Long>, JpaSpecificationExecutor<ContaPagar> {

    List<ContaPagar> findByEmpresaIdAndFilialIdOrderByVencimentoDesc(Long empresaId, Long filialId);

    boolean existsByDescricaoAndEmpresaIdAndFilialId(String descricao, Long empresaId, Long filialId);

    Optional<ContaPagar> findByIdContaPagarAndEmpresaIdAndFilialId(
            Long idContaPagar, Long empresaId, Long filialId);

    List<ContaPagar> findByEmpresaIdAndFilialIdAndStatusOrderByVencimentoAsc(
            Long empresaId, Long filialId, FinanceiroStatus status);

    List<ContaPagar> findByEmpresaIdAndFilialIdAndDataCompetenciaBetween(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim);

    List<ContaPagar> findByEmpresaIdAndFilialIdAndDataCompetenciaBetweenAndStatusIn(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim, List<FinanceiroStatus> statuses);

    List<ContaPagar> findByEmpresaIdAndFilialIdAndPagoEmBetweenAndStatus(
            Long empresaId, Long filialId, LocalDateTime inicio, LocalDateTime fim, FinanceiroStatus status);

    List<ContaPagar> findByEmpresaIdAndFilialIdAndVencimentoBetweenAndStatusIn(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim, List<FinanceiroStatus> statuses);

    List<ContaPagar> findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(Long empresaId, Long filialId);
}
