package Repository;

import Model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    List<Venda> findByDataVendaBetweenAndStatus(LocalDateTime inicio, LocalDateTime fim, int status);

    List<Venda> findByEmpresaIdAndFilialIdOrderByDataVendaDesc(Long empresaId, Long filialId);

    Optional<Venda> findByIdVendaAndEmpresaIdAndFilialId(Long idVenda, Long empresaId, Long filialId);

    List<Venda> findByEmpresaIdAndFilialIdAndDataVendaBetweenAndStatus(
            Long empresaId,
            Long filialId,
            LocalDateTime inicio,
            LocalDateTime fim,
            int status);
}
