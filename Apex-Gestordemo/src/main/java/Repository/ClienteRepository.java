package Repository;

import Model.Cliente;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Override
    @EntityGraph(attributePaths = "enderecos")
    List<Cliente> findAll();

    @Override
    @EntityGraph(attributePaths = "enderecos")
    Optional<Cliente> findById(Long id);

    @EntityGraph(attributePaths = "enderecos")
    List<Cliente> findByEmpresaIdAndFilialIdAndDeletedAtIsNullOrderByNomeRazaoAsc(Long empresaId, Long filialId);

    @EntityGraph(attributePaths = "enderecos")
    Optional<Cliente> findByIdClienteAndEmpresaIdAndFilialId(Long idCliente, Long empresaId, Long filialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "enderecos")
    Optional<Cliente> findWithLockByIdClienteAndEmpresaIdAndFilialId(Long idCliente, Long empresaId, Long filialId);

    Optional<Cliente> findByCpfCnpj(String cpfCnpj);
}
