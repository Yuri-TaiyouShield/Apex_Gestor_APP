package Repository;

import Model.ParametroInteligenciaAdministrativa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametroInteligenciaAdministrativaRepository
        extends JpaRepository<ParametroInteligenciaAdministrativa, Long> {

    Optional<ParametroInteligenciaAdministrativa> findByEmpresaIdAndFilialId(Long empresaId, Long filialId);
}
