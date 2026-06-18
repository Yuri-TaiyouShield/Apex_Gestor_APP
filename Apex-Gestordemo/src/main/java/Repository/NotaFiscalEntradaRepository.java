package Repository;

import Model.NotaFiscalEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotaFiscalEntradaRepository extends JpaRepository<NotaFiscalEntrada, Long> {
    List<NotaFiscalEntrada> findByEmpresaIdAndFilialIdOrderByDataEntradaDesc(Long empresaId, Long filialId);
    Optional<NotaFiscalEntrada> findByIdNfEntradaAndEmpresaIdAndFilialId(Long id, Long empresaId, Long filialId);
}
