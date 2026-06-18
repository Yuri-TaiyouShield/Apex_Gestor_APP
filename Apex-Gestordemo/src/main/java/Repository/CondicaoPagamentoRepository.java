package Repository;

import Model.CondicaoPagamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CondicaoPagamentoRepository extends JpaRepository<CondicaoPagamento, Long> {

    List<CondicaoPagamento> findByEmpresaIdAndFilialIdOrderByNomeAsc(Long empresaId, Long filialId);
    Optional<CondicaoPagamento> findByNomeAndEmpresaIdAndFilialId(String nome, Long empresaId, Long filialId);
}
