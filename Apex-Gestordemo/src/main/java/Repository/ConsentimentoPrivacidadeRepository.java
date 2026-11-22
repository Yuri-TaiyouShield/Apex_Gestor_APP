package Repository;

import Model.ConsentimentoPrivacidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentimentoPrivacidadeRepository extends JpaRepository<ConsentimentoPrivacidade, Long> {

    List<ConsentimentoPrivacidade> findByTitularIdAndTipoTitularAndFinalidadeAndStatus(
            String titularId,
            String tipoTitular,
            String finalidade,
            String status);
}
