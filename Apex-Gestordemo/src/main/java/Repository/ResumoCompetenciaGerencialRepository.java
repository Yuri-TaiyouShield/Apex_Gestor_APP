package Repository;

import Model.ResumoCompetenciaGerencial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumoCompetenciaGerencialRepository
        extends JpaRepository<ResumoCompetenciaGerencial, Long> {

    List<ResumoCompetenciaGerencial> findByEmpresaIdAndFilialIdAndCompetenciaAnoMesOrderByPlanoContaIdAscCentroCustoIdAsc(
            Long empresaId, Long filialId, String competenciaAnoMes);

    List<ResumoCompetenciaGerencial> findByEmpresaIdAndFilialIdAndCompetenciaAnoMesBetweenOrderByCompetenciaAnoMesAsc(
            Long empresaId, Long filialId, String inicioAnoMes, String fimAnoMes);

    Optional<ResumoCompetenciaGerencial> findByEmpresaIdAndFilialIdAndCompetenciaAnoMesAndPlanoContaIdAndCentroCustoId(
            Long empresaId, Long filialId, String competenciaAnoMes, Long planoContaId, Long centroCustoId);

    void deleteByEmpresaIdAndFilialIdAndCompetenciaAnoMes(
            Long empresaId, Long filialId, String competenciaAnoMes);
}
