package Repository;

import Model.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>{

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
    
    Optional<Cliente> findByCpfCnpj(String cpfCnpj);
}
