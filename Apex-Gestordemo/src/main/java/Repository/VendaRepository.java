package Repository;

import Model.Venda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @EntityGraph(attributePaths = {"itens"})
    @Query("SELECT v FROM Venda v WHERE v.dataVenda BETWEEN :inicio AND :fim AND v.status = 1")
    List<Venda> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    @Query("SELECT v FROM Venda v WHERE v.idVenda = :id")
    Optional<Venda> findWithItensById(@Param("id") Long id);
}
