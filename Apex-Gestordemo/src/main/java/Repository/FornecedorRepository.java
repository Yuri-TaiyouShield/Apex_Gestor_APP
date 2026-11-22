package Repository;

import Model.Fornecedor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Optional<Fornecedor> findByCnpj(String cnpj);

    List<Fornecedor> findByEmpresaIdAndFilialIdOrderByRazaoSocialAsc(Long empresaId, Long filialId);

    Optional<Fornecedor> findByIdFornecedorAndEmpresaIdAndFilialId(Long idFornecedor, Long empresaId, Long filialId);
}
