package Repository;

import Model.SolicitacaoTitularDados;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitacaoTitularDadosRepository extends JpaRepository<SolicitacaoTitularDados, Long> {

    List<SolicitacaoTitularDados> findByTitularIdAndTipoTitular(String titularId, String tipoTitular);
}
