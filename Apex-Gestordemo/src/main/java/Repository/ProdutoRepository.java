package Repository;

import Model.Produto;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produto p WHERE p.idProduto IN :ids")
    List<Produto> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}
