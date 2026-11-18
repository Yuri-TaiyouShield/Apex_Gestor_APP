package Repository;

import Model.CreditoCliente;
import Model.CreditoStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface CreditoClienteRepository extends JpaRepository<CreditoCliente, Long> {
    List<CreditoCliente> findByClienteIdAndEmpresaIdAndFilialIdAndStatusInOrderByCriadoEmAsc(
            Long clienteId, Long empresaId, Long filialId, List<CreditoStatus> status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CreditoCliente> findByIdCreditoAndClienteIdAndEmpresaIdAndFilialId(
            Long idCredito, Long clienteId, Long empresaId, Long filialId);

    default Optional<CreditoCliente> lockForUse(Long idCredito, Long clienteId, Long empresaId, Long filialId) {
        return findByIdCreditoAndClienteIdAndEmpresaIdAndFilialId(idCredito, clienteId, empresaId, filialId);
    }
    Optional<CreditoCliente> findByDevolucaoIdAndEmpresaIdAndFilialId(Long devolucaoId, Long empresaId, Long filialId);
}
