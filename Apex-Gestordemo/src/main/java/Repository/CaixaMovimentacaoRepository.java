package Repository;

import Model.CaixaMovimentacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaixaMovimentacaoRepository extends JpaRepository<CaixaMovimentacao, Long> {

    List<CaixaMovimentacao> findByCaixaSessaoIdCaixaSessaoOrderByCriadoEmDesc(Long caixaSessaoId);
}
