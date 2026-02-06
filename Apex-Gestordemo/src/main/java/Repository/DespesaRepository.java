package Repository;

import Model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    @Query("SELECT d FROM Despesa d WHERE d.dataVencimento BETWEEN :inicio AND :fim AND d.status = 1")
    List<Despesa> findByDataVencimentoBetween(LocalDate inicio, LocalDate fim);
}
