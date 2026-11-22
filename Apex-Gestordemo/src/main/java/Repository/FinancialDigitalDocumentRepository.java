package Repository;

import Model.FinancialDigitalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinancialDigitalDocumentRepository extends JpaRepository<FinancialDigitalDocument, Long> {
    Page<FinancialDigitalDocument> findByEmpresaIdAndFilialId(Long empresaId, Long filialId, Pageable pageable);
    Optional<FinancialDigitalDocument> findByIdFinancialDigitalDocumentAndEmpresaIdAndFilialId(
            Long id, Long empresaId, Long filialId);
}
