package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "Credito_Cliente")
@Data
public class CreditoCliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_credito")
    private Long idCredito;
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
    @Column(name = "venda_origem_id", nullable = false)
    private Long vendaOrigemId;
    @Column(name = "devolucao_id", nullable = false)
    private Long devolucaoId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Column(name = "valor_original", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorOriginal;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CreditoStatus status;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
