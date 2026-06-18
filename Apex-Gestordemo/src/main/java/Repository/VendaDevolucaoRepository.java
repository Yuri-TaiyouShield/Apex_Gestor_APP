package Repository;

import Model.VendaDevolucao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaDevolucaoRepository extends JpaRepository<VendaDevolucao, Long> {
    List<VendaDevolucao> findByVendaOriginalIdAndEmpresaIdAndFilialIdOrderByCriadoEmDesc(
            Long vendaOriginalId, Long empresaId, Long filialId);
}
