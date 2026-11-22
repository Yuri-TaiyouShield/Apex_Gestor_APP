package Repository;

import Model.ContaFinanceira;
import Model.TipoContaFinanceira;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContaFinanceiraRepository
        extends JpaRepository<ContaFinanceira, Long>, JpaSpecificationExecutor<ContaFinanceira> {

    List<ContaFinanceira> findByEmpresaIdAndFilialIdOrderByPadraoDescNomeAsc(Long empresaId, Long filialId);

    List<ContaFinanceira> findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPadraoDescNomeAsc(Long empresaId, Long filialId);

    List<ContaFinanceira> findByEmpresaIdAndAtivoTrueOrderByFilialIdAscPadraoDescNomeAsc(Long empresaId);

    Optional<ContaFinanceira> findByIdContaFinanceiraAndEmpresaIdAndFilialId(
            Long idContaFinanceira, Long empresaId, Long filialId);

    Optional<ContaFinanceira> findByCodigoAndEmpresaIdAndFilialId(
            String codigo, Long empresaId, Long filialId);

    Optional<ContaFinanceira> findFirstByEmpresaIdAndFilialIdAndPadraoTrueAndAtivoTrueOrderByIdContaFinanceiraAsc(
            Long empresaId, Long filialId);

    Optional<ContaFinanceira> findFirstByEmpresaIdAndFilialIdAndTipoContaAndAtivoTrueOrderByIdContaFinanceiraAsc(
            Long empresaId, Long filialId, TipoContaFinanceira tipoConta);
}
