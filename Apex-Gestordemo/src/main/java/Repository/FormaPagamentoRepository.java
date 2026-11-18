package Repository;

import Model.FormaPagamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, Long>{

    List<FormaPagamento> findByEmpresaIdAndFilialIdOrderByNomeAsc(Long empresaId, Long filialId);
    List<FormaPagamento> findByEmpresaIdIsNullOrderByNomeAsc();
    Optional<FormaPagamento> findByIdFormaPagamentoAndEmpresaIdAndFilialId(Long id, Long empresaId, Long filialId);
    Optional<FormaPagamento> findByIdFormaPagamentoAndEmpresaIdIsNull(Long id);

    default List<FormaPagamento> findVisibleToTenant(Long empresaId, Long filialId) {
        List<FormaPagamento> result = new java.util.ArrayList<>(findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId));
        result.addAll(findByEmpresaIdIsNullOrderByNomeAsc());
        return result;
    }

    default Optional<FormaPagamento> findVisibleById(Long id, Long empresaId, Long filialId) {
        return findByIdFormaPagamentoAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .or(() -> findByIdFormaPagamentoAndEmpresaIdIsNull(id));
    }

    Optional<FormaPagamento> findByNomeAndEmpresaIdAndFilialId(String nome, Long empresaId, Long filialId);
    
}
