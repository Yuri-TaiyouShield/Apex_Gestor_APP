package Repository;

import Model.ContaReceber;
import Model.FinanceiroStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContaReceberRepository
        extends JpaRepository<ContaReceber, Long>, JpaSpecificationExecutor<ContaReceber> {

    Optional<ContaReceber> findByVendaIdAndEmpresaIdAndFilialId(Long vendaId, Long empresaId, Long filialId);

    Optional<ContaReceber> findFirstByVendaIdAndEmpresaIdAndFilialIdOrderByIdContaReceberAsc(
            Long vendaId, Long empresaId, Long filialId);

    List<ContaReceber> findAllByVendaIdAndEmpresaIdAndFilialIdOrderByNumeroParcelaAscIdContaReceberAsc(
            Long vendaId, Long empresaId, Long filialId);

    List<ContaReceber> findByEmpresaIdAndFilialIdOrderByVencimentoDesc(Long empresaId, Long filialId);

    Optional<ContaReceber> findByIdContaReceberAndEmpresaIdAndFilialId(
            Long idContaReceber, Long empresaId, Long filialId);

    List<ContaReceber> findByEmpresaIdAndFilialIdAndStatusOrderByVencimentoAsc(
            Long empresaId, Long filialId, FinanceiroStatus status);

    List<ContaReceber> findByEmpresaIdAndFilialIdAndDataCompetenciaBetween(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim);

    List<ContaReceber> findByEmpresaIdAndFilialIdAndDataCompetenciaBetweenAndStatusIn(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim, List<FinanceiroStatus> statuses);

    List<ContaReceber> findByEmpresaIdAndFilialIdAndPagoEmBetweenAndStatus(
            Long empresaId, Long filialId, LocalDateTime inicio, LocalDateTime fim, FinanceiroStatus status);

    List<ContaReceber> findByEmpresaIdAndFilialIdAndVencimentoBetweenAndStatusIn(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim, List<FinanceiroStatus> statuses);

    List<ContaReceber> findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(Long empresaId, Long filialId);

    long countByClienteIdAndEmpresaIdAndFilialIdAndStatusIn(
            Long clienteId, Long empresaId, Long filialId, List<FinanceiroStatus> statuses);
}
