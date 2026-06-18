package Repository;

import Model.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>{

    @Query("""
            select c from Categoria c
            where (c.empresaId = :empresaId and c.filialId = :filialId)
               or c.empresaId is null
            order by c.descricao asc
            """)
    List<Categoria> findVisibleToTenant(@Param("empresaId") Long empresaId, @Param("filialId") Long filialId);

    @Query("""
            select c from Categoria c
            where c.idCategoria = :id
              and ((c.empresaId = :empresaId and c.filialId = :filialId) or c.empresaId is null)
            """)
    Optional<Categoria> findVisibleById(@Param("id") Long id, @Param("empresaId") Long empresaId,
            @Param("filialId") Long filialId);
    
}
