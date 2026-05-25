package Repository;

import Model.Venda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @EntityGraph(attributePaths = {"itens"})
    List<Venda> findByDataVendaBetweenAndStatus(LocalDateTime inicio, LocalDateTime fim, int status);

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    Optional<Venda> findByIdVenda(Long id);
}

