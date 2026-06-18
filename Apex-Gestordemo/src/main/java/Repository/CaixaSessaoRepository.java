package Repository;

import Model.CaixaSessao;
import Model.CaixaStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaixaSessaoRepository extends JpaRepository<CaixaSessao, Long> {

    Optional<CaixaSessao> findFirstByOperadorLoginAndEmpresaIdAndFilialIdAndStatusOrderByAbertoEmDesc(
            String operadorLogin,
            Long empresaId,
            Long filialId,
            CaixaStatus status);

    List<CaixaSessao> findTop20ByEmpresaIdAndFilialIdOrderByAbertoEmDesc(Long empresaId, Long filialId);
}
