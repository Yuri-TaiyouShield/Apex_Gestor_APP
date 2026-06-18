package Repository;

import Model.ContaPagar;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaPagarRepository extends JpaRepository<ContaPagar, Long> {
    List<ContaPagar> findByEmpresaIdAndFilialIdOrderByVencimentoDesc(Long empresaId, Long filialId);
    boolean existsByDescricaoAndEmpresaIdAndFilialId(String descricao, Long empresaId, Long filialId);
}
