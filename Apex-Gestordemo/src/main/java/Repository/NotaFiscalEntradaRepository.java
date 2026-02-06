package Repository;

import Model.NotaFiscalEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotaFiscalEntradaRepository extends JpaRepository<NotaFiscalEntrada, Long> {
}