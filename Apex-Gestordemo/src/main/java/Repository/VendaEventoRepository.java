package Repository;

import Model.VendaEvento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaEventoRepository extends JpaRepository<VendaEvento, Long> {
    List<VendaEvento> findByVendaIdAndEmpresaIdAndFilialIdOrderByCriadoEmAsc(
            Long vendaId, Long empresaId, Long filialId);
}
