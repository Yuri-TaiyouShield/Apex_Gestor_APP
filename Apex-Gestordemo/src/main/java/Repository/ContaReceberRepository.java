package Repository;

import Model.ContaReceber;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {
    Optional<ContaReceber> findByVendaIdAndEmpresaIdAndFilialId(Long vendaId, Long empresaId, Long filialId);
    List<ContaReceber> findByEmpresaIdAndFilialIdOrderByVencimentoDesc(Long empresaId, Long filialId);
}
