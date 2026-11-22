package Repository;

import Model.GrupoDreGerencial;
import Model.PlanoContasGerencial;
import Model.TipoContaGerencial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanoContasGerencialRepository
        extends JpaRepository<PlanoContasGerencial, Long>, JpaSpecificationExecutor<PlanoContasGerencial> {

    List<PlanoContasGerencial> findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(
            Long empresaId, Long filialId);

    List<PlanoContasGerencial> findByEmpresaIdAndFilialIdAndAtivoTrueOrderByOrdemExibicaoAscCodigoAsc(
            Long empresaId, Long filialId);

    List<PlanoContasGerencial> findByEmpresaIdAndFilialIdAndGrupoDreAndAtivoTrueOrderByOrdemExibicaoAscCodigoAsc(
            Long empresaId, Long filialId, GrupoDreGerencial grupoDre);

    List<PlanoContasGerencial> findByEmpresaIdAndFilialIdAndTipoContaAndAtivoTrueOrderByOrdemExibicaoAscCodigoAsc(
            Long empresaId, Long filialId, TipoContaGerencial tipoConta);

    List<PlanoContasGerencial> findByEmpresaIdAndFilialIdAndContaPaiIdOrderByOrdemExibicaoAscCodigoAsc(
            Long empresaId, Long filialId, Long contaPaiId);

    Optional<PlanoContasGerencial> findByIdPlanoContaAndEmpresaIdAndFilialId(
            Long idPlanoConta, Long empresaId, Long filialId);

    Optional<PlanoContasGerencial> findByCodigoAndEmpresaIdAndFilialId(
            String codigo, Long empresaId, Long filialId);

    boolean existsByEmpresaIdAndFilialId(Long empresaId, Long filialId);

    boolean existsByCodigoAndEmpresaIdAndFilialId(String codigo, Long empresaId, Long filialId);

    boolean existsByCodigoAndEmpresaIdAndFilialIdAndIdPlanoContaNot(
            String codigo, Long empresaId, Long filialId, Long idPlanoConta);

    boolean existsByContaPaiIdAndEmpresaIdAndFilialId(Long contaPaiId, Long empresaId, Long filialId);
}
