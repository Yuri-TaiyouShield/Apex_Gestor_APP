package Service;

import DTO.AuthResponseDTO;
import Model.Usuario;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secret;

    @Value("${apex.jwt.issuer:apex-gestor}")
    private String issuer = "apex-gestor";

    @Value("${apex.jwt.audience:apex-api}")
    private String audience = "apex-api";

    public JwtTokenService(@Value("${apex.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank() || secret.length() < 32
                || secret.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalStateException(
                    "apex.jwt.secret deve existir, ter ao menos 32 caracteres e nao conter espacos.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issueAccessToken(Long userId,
            String nome,
            String login,
            List<String> roles,
            Long empresaId,
            Long filialId,
            Duration ttl) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", login);
        payload.put("uid", userId);
        payload.put("name", nome);
        payload.put("login", login);
        payload.put("roles", roles);
        payload.put("empresaId", empresaId);
        payload.put("filialId", filialId);
        payload.put("iss", issuer);
        payload.put("aud", audience);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(ttl).getEpochSecond());
        payload.put("sid", java.util.UUID.randomUUID().toString());
        return sign(payload);
    }

    public JwtClaims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token ausente.");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token invalido.");
        }
        String signed = parts[0] + "." + parts[1];
        String expected = base64Url(hmac(signed));
        if (!constantTimeEquals(expected, parts[2])) {
            throw new IllegalArgumentException("Assinatura JWT invalida.");
        }
        Map<String, Object> header = readJson(parts[0]);
        if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
            throw new IllegalArgumentException("Algoritmo JWT nao permitido.");
        }
        Map<String, Object> payload = readJson(parts[1]);
        long exp = number(payload.get("exp"));
        long iat = number(payload.get("iat"));
        if (exp <= Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("Token expirado.");
        }
        if (iat <= 0 || iat > Instant.now().plusSeconds(60).getEpochSecond()
                || !issuer.equals(string(payload.get("iss")))
                || !audience.equals(string(payload.get("aud")))) {
            throw new IllegalArgumentException("Claims JWT invalidas.");
        }
        return new JwtClaims(
                string(payload.get("login")),
                numberObject(payload.get("uid")),
                strings(payload.get("roles")),
                numberObject(payload.get("empresaId")),
                numberObject(payload.get("filialId")),
                exp);
    }

    public AuthResponseDTO response(Usuario usuario, String refreshToken) {
        Duration ttl = Duration.ofMinutes(30);
        List<String> roles = roles(usuario);
        String accessToken = issueAccessToken(usuario.getIdUsuario(), usuario.getNome(), usuario.getLogin(), roles,
                usuario.getEmpresaId(), usuario.getFilialId(), ttl);
        return new AuthResponseDTO(accessToken, refreshToken, Instant.now().plus(ttl), "Bearer",
                usuario.getIdUsuario(), usuario.getNome(), usuario.getLogin(), roles);
    }

    private List<String> roles(Usuario usuario) {
        String perfil = usuario.getPerfil() == null ? "VENDEDOR" : usuario.getPerfil().getNome();
        String normalized = Normalizer.normalize(perfil, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "_")
                .toUpperCase(Locale.ROOT);
        String role = normalized.isBlank() ? "VENDEDOR" : normalized;
        return List.of(role.startsWith("ROLE_") ? role : "ROLE_" + role);
    }

    private String sign(Map<String, Object> payload) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        String encodedHeader = base64Url(writeJson(header));
        String encodedPayload = base64Url(writeJson(payload));
        String signed = encodedHeader + "." + encodedPayload;
        return signed + "." + base64Url(hmac(signed));
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao assinar JWT.", ex);
        }
    }

    private byte[] writeJson(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar JWT.", ex);
        }
    }

    private Map<String, Object> readJson(String encoded) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Payload JWT invalido.", ex);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigestCompat.equals(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Object value) {
        Long parsed = numberObject(value);
        return parsed == null ? 0L : parsed;
    }

    private static Long numberObject(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public record JwtClaims(String login, Long userId, List<String> roles, Long empresaId, Long filialId, long expiresAt) {
    }

    private static final class MessageDigestCompat {
        private static boolean equals(byte[] left, byte[] right) {
            if (left.length != right.length) {
                return false;
            }
            int diff = 0;
            for (int i = 0; i < left.length; i++) {
                diff |= left[i] ^ right[i];
            }
            return diff == 0;
        }
    }
}
