package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "Credito_Movimentacao")
@Data
public class CreditoMovimentacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_credito_movimentacao")
    private Long idCreditoMovimentacao;
    @Column(name = "credito_id", nullable = false)
    private Long creditoId;
    @Column(name = "venda_id")
    private Long vendaId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CreditoMovimentoTipo tipo;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;
    @Column(name = "usuario_login", length = 80)
    private String usuarioLogin;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
