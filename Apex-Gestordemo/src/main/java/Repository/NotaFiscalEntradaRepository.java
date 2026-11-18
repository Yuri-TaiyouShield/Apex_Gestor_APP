package Repository;

import Model.NotaFiscalEntrada;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NotaFiscalEntradaRepository
        extends JpaRepository<NotaFiscalEntrada, Long>, JpaSpecificationExecutor<NotaFiscalEntrada> {

    List<NotaFiscalEntrada> findByEmpresaIdAndFilialIdOrderByDataEntradaDesc(Long empresaId, Long filialId);

    Optional<NotaFiscalEntrada> findByIdNfEntradaAndEmpresaIdAndFilialId(Long id, Long empresaId, Long filialId);

    Optional<NotaFiscalEntrada> findByIdempotencyKeyAndEmpresaIdAndFilialId(
            String idempotencyKey, Long empresaId, Long filialId);

    List<NotaFiscalEntrada> findByEmpresaIdAndFilialIdAndDataCompetenciaBetweenOrderByDataCompetenciaDesc(
            Long empresaId, Long filialId, LocalDate inicio, LocalDate fim);

    Optional<NotaFiscalEntrada> findByFornecedorIdFornecedorAndNumeroAndSerieAndEmpresaIdAndFilialId(
            Long fornecedorId, String numero, String serie, Long empresaId, Long filialId);
}
