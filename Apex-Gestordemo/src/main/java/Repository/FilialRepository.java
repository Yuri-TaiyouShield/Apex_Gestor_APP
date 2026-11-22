package Repository;

import Model.Filial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilialRepository extends JpaRepository<Filial, Long> {
    Optional<Filial> findByEmpresaIdEmpresaAndCodigo(Long empresaId, String codigo);

    List<Filial> findByEmpresaIdEmpresaAndDeletedAtIsNullOrderByIdFilialAsc(Long empresaId);

    Optional<Filial> findByIdFilialAndEmpresaIdEmpresa(Long idFilial, Long empresaId);
}
