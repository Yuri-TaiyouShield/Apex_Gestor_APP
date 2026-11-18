package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Nota_Fiscal_Entrada", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nf_entrada_tenant_idempotency", columnNames = {"empresa_id", "filial_id", "idempotency_key"}),
        @UniqueConstraint(name = "uk_nf_entrada_tenant_natural", columnNames = {"empresa_id", "filial_id", "fornecedor_id", "numero", "serie"})
}, indexes = {
        @Index(name = "idx_nf_entrada_tenant_entrada", columnList = "empresa_id, filial_id, data_entrada"),
        @Index(name = "idx_nf_entrada_classificacao", columnList = "empresa_id, filial_id, plano_conta_id, centro_custo_id")
})
@Data
public class NotaFiscalEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nf_entrada")
    private Long idNfEntrada;
    @Column(nullable = false, length = 20)
    private String numero;
    @Column(length = 5)
    private String serie;
    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;
    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao;
    @Column(name = "data_entrada", nullable = false)
    private LocalDateTime dataEntrada;
    @Column(name = "data_competencia")
    private LocalDate dataCompetencia;
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(name = "filial_id")
    private Long filialId;
    @Column(name = "plano_conta_id")
    private Long planoContaId;
    @Column(name = "centro_custo_id")
    private Long centroCustoId;
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", length = 64)
    private String idempotencyPayloadHash;
    @Version
    @Column(nullable = false)
    private Long version;
    @OneToMany(mappedBy = "notaFiscal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemNotaFiscal> itens;
}
