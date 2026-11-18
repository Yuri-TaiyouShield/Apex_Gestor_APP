package Repository;

import Model.VendaDevolucao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaDevolucaoRepository extends JpaRepository<VendaDevolucao, Long> {
    Optional<VendaDevolucao> findByIdempotencyKeyAndEmpresaIdAndFilialId(
            String idempotencyKey, Long empresaId, Long filialId);

    List<VendaDevolucao> findByVendaOriginalIdAndEmpresaIdAndFilialIdOrderByCriadoEmDesc(
            Long vendaOriginalId, Long empresaId, Long filialId);

    List<VendaDevolucao> findByEmpresaIdAndFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Long empresaId, Long filialId, LocalDateTime inicio, LocalDateTime fim);
}
