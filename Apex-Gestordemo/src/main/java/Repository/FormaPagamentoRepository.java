package Repository;

import Model.FormaPagamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, Long>{

    @Query("""
            select f from FormaPagamento f
            where (f.empresaId = :empresaId and f.filialId = :filialId)
               or f.empresaId is null
            order by f.nome asc
            """)
    List<FormaPagamento> findVisibleToTenant(@Param("empresaId") Long empresaId, @Param("filialId") Long filialId);

    @Query("""
            select f from FormaPagamento f
            where f.idFormaPagamento = :id
              and ((f.empresaId = :empresaId and f.filialId = :filialId) or f.empresaId is null)
            """)
    Optional<FormaPagamento> findVisibleById(@Param("id") Long id, @Param("empresaId") Long empresaId,
            @Param("filialId") Long filialId);
    
}
