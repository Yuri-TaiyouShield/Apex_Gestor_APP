package Repository;

import Model.CreditoMovimentacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditoMovimentacaoRepository extends JpaRepository<CreditoMovimentacao, Long> {
    List<CreditoMovimentacao> findByCreditoIdAndEmpresaIdAndFilialIdOrderByCriadoEmAsc(
            Long creditoId, Long empresaId, Long filialId);
}
