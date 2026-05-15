package Repository;

import Model.B2cCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface B2cCartRepository extends JpaRepository<B2cCart, Long> {

    Optional<B2cCart> findByClienteIdClienteAndStatus(Long clienteId, String status);
}
