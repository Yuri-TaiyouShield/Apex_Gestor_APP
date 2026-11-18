package Repository;

import Model.Venda;
import Model.VendaStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long>, JpaSpecificationExecutor<Venda> {

    List<Venda> findByDataVendaBetweenAndStatus(LocalDateTime inicio, LocalDateTime fim, int status);

    List<Venda> findByEmpresaIdAndFilialIdOrderByDataVendaDesc(Long empresaId, Long filialId);

    List<Venda> findByEmpresaIdAndFilialIdAndStatusOperacionalOrderByEnviadoCaixaEmAsc(
            Long empresaId, Long filialId, VendaStatus statusOperacional);

    List<Venda> findByUsuarioIdUsuarioAndEmpresaIdAndFilialIdAndDataVendaBetweenOrderByDataVendaDesc(
            Long usuarioId, Long empresaId, Long filialId, LocalDateTime inicio, LocalDateTime fim);

    List<Venda> findTop10ByUsuarioIdUsuarioAndEmpresaIdAndFilialIdOrderByDataVendaDesc(
            Long usuarioId, Long empresaId, Long filialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Venda> findByIdVendaAndEmpresaIdAndFilialId(Long idVenda, Long empresaId, Long filialId);

    Optional<Venda> findByIdempotencyKeyAndEmpresaIdAndFilialId(String idempotencyKey, Long empresaId, Long filialId);

    Optional<Venda> findByRecebimentoIdempotencyKeyAndEmpresaIdAndFilialId(
            String idempotencyKey, Long empresaId, Long filialId);

    List<Venda> findByEmpresaIdAndFilialIdAndDataVendaBetweenAndStatus(
            Long empresaId,
            Long filialId,
            LocalDateTime inicio,
            LocalDateTime fim,
            int status);

    List<Venda> findByEmpresaIdAndFilialIdAndDataCompetenciaBetweenAndStatus(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            int status);

    List<Venda> findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(Long empresaId, Long filialId);
}
