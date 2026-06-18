package Repository;

import Model.CreditoCliente;
import Model.CreditoStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditoClienteRepository extends JpaRepository<CreditoCliente, Long> {
    List<CreditoCliente> findByClienteIdAndEmpresaIdAndFilialIdAndStatusInOrderByCriadoEmAsc(
            Long clienteId, Long empresaId, Long filialId, List<CreditoStatus> status);
    Optional<CreditoCliente> findByIdCreditoAndClienteIdAndEmpresaIdAndFilialId(
            Long idCredito, Long clienteId, Long empresaId, Long filialId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CreditoCliente c where c.idCredito = :id and c.clienteId = :clienteId and c.empresaId = :empresaId and c.filialId = :filialId")
    Optional<CreditoCliente> lockForUse(@Param("id") Long idCredito, @Param("clienteId") Long clienteId,
            @Param("empresaId") Long empresaId, @Param("filialId") Long filialId);
    Optional<CreditoCliente> findByDevolucaoIdAndEmpresaIdAndFilialId(Long devolucaoId, Long empresaId, Long filialId);
}
