package Repository;

import Model.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long>{
    List<Endereco> findByEmpresaIdAndFilialIdOrderByCidadeAsc(Long empresaId, Long filialId);
    Optional<Endereco> findByIdEnderecoAndEmpresaIdAndFilialId(Long idEndereco, Long empresaId, Long filialId);
}
