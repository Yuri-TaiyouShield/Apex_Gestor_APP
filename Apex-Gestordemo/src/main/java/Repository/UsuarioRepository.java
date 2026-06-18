package Repository;

import Model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    List<Usuario> findByEmpresaIdAndFilialIdOrderByNomeAsc(Long empresaId, Long filialId);

    Optional<Usuario> findByIdUsuarioAndEmpresaIdAndFilialId(Long idUsuario, Long empresaId, Long filialId);
}
