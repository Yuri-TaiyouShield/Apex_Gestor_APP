package Repository;

import Model.Despesa;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DespesaRepository
        extends JpaRepository<Despesa, Long>, JpaSpecificationExecutor<Despesa> {

    List<Despesa> findByDataVencimentoBetweenAndStatus(LocalDate inicio, LocalDate fim, int status);

    List<Despesa> findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(Long empresaId, Long filialId);

    Optional<Despesa> findByIdDespesaAndEmpresaIdAndFilialId(Long idDespesa, Long empresaId, Long filialId);

    List<Despesa> findByEmpresaIdAndFilialIdAndDataVencimentoBetweenAndStatus(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            int status);

    List<Despesa> findByEmpresaIdAndFilialIdAndDataCompetenciaBetweenAndStatus(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            int status);

    List<Despesa> findByEmpresaIdAndFilialIdAndDataCompetenciaBetween(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim);

    List<Despesa> findByEmpresaIdAndFilialIdAndDataPagamentoBetweenAndStatus(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            int status);

    List<Despesa> findByEmpresaIdAndFilialIdAndDataPagamentoBetween(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim);

    List<Despesa> findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(Long empresaId, Long filialId);
}
