package Repository;

import Model.CaixaSessao;
import Model.CaixaStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface CaixaSessaoRepository extends JpaRepository<CaixaSessao, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CaixaSessao> findFirstByOperadorLoginAndEmpresaIdAndFilialIdAndStatusOrderByAbertoEmDesc(
            String operadorLogin,
            Long empresaId,
            Long filialId,
            CaixaStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CaixaSessao> findByIdCaixaSessaoAndEmpresaIdAndFilialId(Long idCaixaSessao, Long empresaId, Long filialId);

    List<CaixaSessao> findTop20ByEmpresaIdAndFilialIdOrderByAbertoEmDesc(Long empresaId, Long filialId);
}
