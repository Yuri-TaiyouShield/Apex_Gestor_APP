package Service;

import DTO.AuthResponseDTO;
import Model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    @Value("${apex.security.jwt.issuer}")
    private String issuer;

    @Value("${apex.security.jwt.access-token-minutes}")
    private long accessTokenMinutes;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        List<String> roles = roles(usuario);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
                .subject(usuario.getLogin())
                .claim("uid", usuario.getIdUsuario())
                .claim("name", usuario.getNome())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Instant accessTokenExpiresAt() {
        return Instant.now().plus(accessTokenMinutes, ChronoUnit.MINUTES);
    }

    public List<String> roles(Usuario usuario) {
        String perfil = usuario.getPerfil() != null ? usuario.getPerfil().getNome() : "VENDEDOR";
        String normalized = Normalizer.normalize(perfil, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "_")
                .toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            normalized = "VENDEDOR";
        }
        return List.of(normalized);
    }

    public AuthResponseDTO response(Usuario usuario, String refreshToken) {
        return new AuthResponseDTO(
                generateAccessToken(usuario),
                refreshToken,
                accessTokenExpiresAt(),
                "Bearer",
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getLogin(),
                roles(usuario)
        );
    }
}
