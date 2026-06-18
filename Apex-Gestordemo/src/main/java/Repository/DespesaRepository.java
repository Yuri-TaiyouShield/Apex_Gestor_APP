package Repository;

import Model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findByDataVencimentoBetweenAndStatus(LocalDate inicio, LocalDate fim, int status);

    List<Despesa> findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(Long empresaId, Long filialId);

    Optional<Despesa> findByIdDespesaAndEmpresaIdAndFilialId(Long idDespesa, Long empresaId, Long filialId);

    List<Despesa> findByEmpresaIdAndFilialIdAndDataVencimentoBetweenAndStatus(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            int status);
}
