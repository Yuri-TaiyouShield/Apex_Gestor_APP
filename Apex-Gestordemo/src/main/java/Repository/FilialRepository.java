package Repository;

import Model.Filial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FilialRepository extends JpaRepository<Filial, Long> {
    Optional<Filial> findByEmpresaIdEmpresaAndCodigo(Long empresaId, String codigo);
}
