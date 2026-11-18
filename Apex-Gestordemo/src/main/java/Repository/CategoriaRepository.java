package Repository;

import Model.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>{

    List<Categoria> findByEmpresaIdAndFilialIdOrderByDescricaoAsc(Long empresaId, Long filialId);
    List<Categoria> findByEmpresaIdIsNullOrderByDescricaoAsc();
    Optional<Categoria> findByIdCategoriaAndEmpresaIdAndFilialId(Long id, Long empresaId, Long filialId);
    Optional<Categoria> findByIdCategoriaAndEmpresaIdIsNull(Long id);

    default List<Categoria> findVisibleToTenant(Long empresaId, Long filialId) {
        List<Categoria> result = new java.util.ArrayList<>(findByEmpresaIdAndFilialIdOrderByDescricaoAsc(empresaId, filialId));
        result.addAll(findByEmpresaIdIsNullOrderByDescricaoAsc());
        return result;
    }

    default Optional<Categoria> findVisibleById(Long id, Long empresaId, Long filialId) {
        return findByIdCategoriaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .or(() -> findByIdCategoriaAndEmpresaIdIsNull(id));
    }
    
}
