package Repository;

import Model.RefreshToken;
import Model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUsuarioAndRevokedAtIsNull(Usuario usuario);
    void deleteByUsuario(Usuario usuario);
}
