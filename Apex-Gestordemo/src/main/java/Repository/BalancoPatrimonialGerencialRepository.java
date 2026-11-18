package Repository;

import Model.BalancoPatrimonialGerencial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalancoPatrimonialGerencialRepository extends JpaRepository<BalancoPatrimonialGerencial, Long> {

    Optional<BalancoPatrimonialGerencial> findByEmpresaIdAndFilialIdAndCompetenciaAnoMes(
            Long empresaId, Long filialId, String competenciaAnoMes);

    List<BalancoPatrimonialGerencial> findByEmpresaIdAndFilialIdOrderByCompetenciaAnoMesDesc(
            Long empresaId, Long filialId);

    List<BalancoPatrimonialGerencial> findByEmpresaIdAndCompetenciaAnoMes(
            Long empresaId, String competenciaAnoMes);
}
