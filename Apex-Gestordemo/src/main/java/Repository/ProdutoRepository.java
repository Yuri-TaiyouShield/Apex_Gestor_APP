package Repository;

import Model.Produto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByEmpresaIdAndFilialIdOrderByDescricaoAsc(Long empresaId, Long filialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Produto> findByIdProdutoAndEmpresaIdAndFilialId(Long idProduto, Long empresaId, Long filialId);

    Optional<Produto> findByCodigoBarrasAndEmpresaIdAndFilialId(String codigoBarras, Long empresaId, Long filialId);
}
