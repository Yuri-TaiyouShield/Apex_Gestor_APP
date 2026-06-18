package Repository;

import Model.ProdutoVenda;
import Model.ProdutoVendaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoVendaRepository extends JpaRepository<ProdutoVenda, ProdutoVendaId> {
    List<ProdutoVenda> findByVendaEmpresaIdAndVendaFilialId(Long empresaId, Long filialId);
    Optional<ProdutoVenda> findByIdAndVendaEmpresaIdAndVendaFilialId(
            ProdutoVendaId id, Long empresaId, Long filialId);
}
