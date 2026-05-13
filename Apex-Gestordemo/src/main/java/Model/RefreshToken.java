package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true)
})
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refresh_token")
    private Long idRefreshToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "user_agent_hash", length = 128)
    private String userAgentHash;
}
