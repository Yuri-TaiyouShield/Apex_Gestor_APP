package Repository;

import Model.TipoDespesa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDespesaRepository
        extends JpaRepository<TipoDespesa, Long>, JpaSpecificationExecutor<TipoDespesa> {

    List<TipoDespesa> findByEmpresaIdAndFilialIdOrderByNomeAsc(Long empresaId, Long filialId);

    List<TipoDespesa> findByEmpresaIdIsNullOrderByNomeAsc();

    List<TipoDespesa> findByEmpresaIdAndFilialIdAndStatusOrderByNomeAsc(Long empresaId, Long filialId, int status);

    List<TipoDespesa> findByEmpresaIdIsNullAndStatusOrderByNomeAsc(int status);

    Optional<TipoDespesa> findByIdTipoDespesaAndEmpresaIdAndFilialId(Long idTipoDespesa, Long empresaId, Long filialId);

    Optional<TipoDespesa> findByIdTipoDespesaAndEmpresaIdIsNull(Long idTipoDespesa);

    Optional<TipoDespesa> findByNomeIgnoreCaseAndEmpresaIdAndFilialId(String nome, Long empresaId, Long filialId);

    Optional<TipoDespesa> findByNomeIgnoreCaseAndEmpresaIdIsNull(String nome);

    boolean existsByNomeIgnoreCaseAndEmpresaIdAndFilialId(String nome, Long empresaId, Long filialId);

    boolean existsByNomeIgnoreCaseAndEmpresaIdAndFilialIdAndIdTipoDespesaNot(
            String nome, Long empresaId, Long filialId, Long idTipoDespesa);

    default List<TipoDespesa> findVisibleToTenant(Long empresaId, Long filialId) {
        List<TipoDespesa> result = new java.util.ArrayList<>(
                findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId));
        result.addAll(findByEmpresaIdIsNullOrderByNomeAsc());
        return result;
    }

    default List<TipoDespesa> findVisibleActiveToTenant(Long empresaId, Long filialId) {
        List<TipoDespesa> result = new java.util.ArrayList<>(
                findByEmpresaIdAndFilialIdAndStatusOrderByNomeAsc(empresaId, filialId, 1));
        result.addAll(findByEmpresaIdIsNullAndStatusOrderByNomeAsc(1));
        return result;
    }

    default Optional<TipoDespesa> findVisibleById(Long id, Long empresaId, Long filialId) {
        return findByIdTipoDespesaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .or(() -> findByIdTipoDespesaAndEmpresaIdIsNull(id));
    }
}
