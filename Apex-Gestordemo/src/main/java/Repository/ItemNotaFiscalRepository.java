package Repository;

import Model.ItemNotaFiscal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemNotaFiscalRepository extends JpaRepository<ItemNotaFiscal, Long> {

    List<ItemNotaFiscal> findByNotaFiscalEmpresaIdAndNotaFiscalFilialId(Long empresaId, Long filialId);

    List<ItemNotaFiscal> findByProdutoIdProdutoAndNotaFiscalEmpresaIdAndNotaFiscalFilialIdOrderByIdItemNfDesc(
            Long produtoId, Long empresaId, Long filialId);
}
