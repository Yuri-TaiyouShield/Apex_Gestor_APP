package Repository;

import Model.GrupoProduto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GrupoProdutoRepository extends JpaRepository<GrupoProduto, Long> {

    @Query("""
            select g from GrupoProduto g
            where (g.empresaId = :empresaId and g.filialId = :filialId)
               or g.empresaId is null
            order by g.nome asc
            """)
    List<GrupoProduto> findVisibleToTenant(@Param("empresaId") Long empresaId, @Param("filialId") Long filialId);

    @Query("""
            select g from GrupoProduto g
            where g.idGrupoProduto = :id
              and ((g.empresaId = :empresaId and g.filialId = :filialId) or g.empresaId is null)
            """)
    Optional<GrupoProduto> findVisibleById(@Param("id") Long id, @Param("empresaId") Long empresaId,
            @Param("filialId") Long filialId);
}
