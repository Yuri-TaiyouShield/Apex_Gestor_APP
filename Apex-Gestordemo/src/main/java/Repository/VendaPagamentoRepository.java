package Repository;

import Model.VendaPagamento;
import Model.VendaPagamentoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface VendaPagamentoRepository extends JpaRepository<VendaPagamento, VendaPagamentoId>{
    List<VendaPagamento> findByVendaEmpresaIdAndVendaFilialId(Long empresaId, Long filialId);
    Optional<VendaPagamento> findByIdAndVendaEmpresaIdAndVendaFilialId(
            VendaPagamentoId id, Long empresaId, Long filialId);
    @EntityGraph(attributePaths = "formaPagamento")
    List<VendaPagamento> findByVendaCaixaSessaoIdAndVendaEmpresaIdAndVendaFilialId(
            Long caixaSessaoId, Long empresaId, Long filialId);
}
