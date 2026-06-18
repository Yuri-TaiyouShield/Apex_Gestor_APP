package Repository;

import Model.MetaVendedor;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaVendedorRepository extends JpaRepository<MetaVendedor, Long> {
    Optional<MetaVendedor> findFirstByUsuarioIdAndEmpresaIdAndFilialIdAndPeriodoInicioLessThanEqualAndPeriodoFimGreaterThanEqualOrderByPeriodoInicioDesc(
            Long usuarioId, Long empresaId, Long filialId, LocalDate periodoInicio, LocalDate periodoFim);
}
