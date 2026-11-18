package Repository;

import Model.GrupoProduto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrupoProdutoRepository extends JpaRepository<GrupoProduto, Long> {

    List<GrupoProduto> findByEmpresaIdAndFilialIdOrderByNomeAsc(Long empresaId, Long filialId);
    List<GrupoProduto> findByEmpresaIdIsNullOrderByNomeAsc();
    Optional<GrupoProduto> findByIdGrupoProdutoAndEmpresaIdAndFilialId(Long id, Long empresaId, Long filialId);
    Optional<GrupoProduto> findByIdGrupoProdutoAndEmpresaIdIsNull(Long id);

    default List<GrupoProduto> findVisibleToTenant(Long empresaId, Long filialId) {
        List<GrupoProduto> result = new java.util.ArrayList<>(findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId));
        result.addAll(findByEmpresaIdIsNullOrderByNomeAsc());
        return result;
    }

    default Optional<GrupoProduto> findVisibleById(Long id, Long empresaId, Long filialId) {
        return findByIdGrupoProdutoAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .or(() -> findByIdGrupoProdutoAndEmpresaIdIsNull(id));
    }
}
