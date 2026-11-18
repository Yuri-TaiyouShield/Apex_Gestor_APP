package Repository;

import Model.PoliticaSegregacaoAprovacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoliticaSegregacaoAprovacaoRepository extends JpaRepository<PoliticaSegregacaoAprovacao, Long> {

    Optional<PoliticaSegregacaoAprovacao> findByEmpresaIdAndFilialIdAndDominioOperacao(
            Long empresaId, Long filialId, String dominioOperacao);

    List<PoliticaSegregacaoAprovacao> findByEmpresaIdAndFilialIdOrderByDominioOperacaoAsc(
            Long empresaId, Long filialId);
}
