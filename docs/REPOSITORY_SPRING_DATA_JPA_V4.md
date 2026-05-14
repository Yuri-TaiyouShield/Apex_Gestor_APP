# Exemplo real de Repository limpo com Spring Data JPA

Regra v4: repositories nao devem conter SQL manual quando a consulta puder ser
expressa por nomenclatura nativa do Spring Data JPA.

Exemplo aplicado no projeto:

```java
package Repository;

import Model.Venda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @EntityGraph(attributePaths = {"itens"})
    List<Venda> findByDataVendaBetweenAndStatus(
            LocalDateTime inicio,
            LocalDateTime fim,
            int status
    );

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    Optional<Venda> findByIdVenda(Long id);
}
```

Por que esta interface esta aderente:

- nao usa `@Query`;
- nao usa SQL nativo;
- nao injeta `EntityManager`;
- deixa o Spring Data derivar a query por `findByDataVendaBetweenAndStatus`;
- usa `@EntityGraph` somente para estrategia de carregamento, sem acoplar SQL.

O gate `node scripts/audit-spring-data-jpa.cjs` falha o CI se algum padrao manual
proibido for reintroduzido.
