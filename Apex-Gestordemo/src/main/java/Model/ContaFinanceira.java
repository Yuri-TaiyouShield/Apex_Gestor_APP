package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "conta_financeira", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conta_fin_tenant_codigo", columnNames = {"empresa_id", "filial_id", "codigo"})
}, indexes = {
        @Index(name = "idx_conta_fin_tenant_ativo", columnList = "empresa_id, filial_id, ativo, tipo_conta")
})
@Data
public class ContaFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conta_financeira")
    private Long idContaFinanceira;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(nullable = false, length = 32)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", nullable = false, length = 32)
    private TipoContaFinanceira tipoConta;

    @Column(name = "banco_codigo", length = 16)
    private String bancoCodigo;

    @Column(name = "banco_nome", length = 80)
    private String bancoNome;

    @Column(length = 24)
    private String agencia;

    @Column(name = "numero_conta", length = 32)
    private String numeroConta;

    @Column(name = "saldo_inicial", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_atual", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoAtual = BigDecimal.ZERO;

    @Column(name = "data_saldo_inicial", nullable = false)
    private LocalDate dataSaldoInicial;

    @Column(nullable = false)
    private boolean padrao = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long version;
}
