package Repository;

import Model.CentroCusto;
import Model.TipoCentroCusto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CentroCustoRepository
        extends JpaRepository<CentroCusto, Long>, JpaSpecificationExecutor<CentroCusto> {

    List<CentroCusto> findByEmpresaIdAndFilialIdOrderByNomeAsc(Long empresaId, Long filialId);

    List<CentroCusto> findByEmpresaIdAndFilialIdAndAtivoTrueOrderByNomeAsc(Long empresaId, Long filialId);

    List<CentroCusto> findByEmpresaIdAndFilialIdAndTipoOperacionalAndAtivoTrueOrderByNomeAsc(
            Long empresaId, Long filialId, TipoCentroCusto tipoOperacional);

    Optional<CentroCusto> findByIdCentroCustoAndEmpresaIdAndFilialId(
            Long idCentroCusto, Long empresaId, Long filialId);

    Optional<CentroCusto> findByCodigoAndEmpresaIdAndFilialId(
            String codigo, Long empresaId, Long filialId);

    boolean existsByEmpresaIdAndFilialId(Long empresaId, Long filialId);

    boolean existsByCodigoAndEmpresaIdAndFilialId(String codigo, Long empresaId, Long filialId);

    boolean existsByCodigoAndEmpresaIdAndFilialIdAndIdCentroCustoNot(
            String codigo, Long empresaId, Long filialId, Long idCentroCusto);
}
