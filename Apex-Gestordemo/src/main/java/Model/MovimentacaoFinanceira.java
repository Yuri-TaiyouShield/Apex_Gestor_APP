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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "movimentacao_financeira", indexes = {
        @Index(name = "idx_mov_fin_tenant_data", columnList = "empresa_id, filial_id, data_movimentacao, tipo_movimentacao"),
        @Index(name = "idx_mov_fin_conta_data", columnList = "empresa_id, filial_id, conta_financeira_id, data_movimentacao"),
        @Index(name = "idx_mov_fin_origem", columnList = "empresa_id, filial_id, origem_tipo, origem_id"),
        @Index(name = "idx_mov_fin_idempotency", columnList = "empresa_id, filial_id, idempotency_key")
})
@Data
public class MovimentacaoFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimentacao_financeira")
    private Long idMovimentacaoFinanceira;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(name = "conta_financeira_id", nullable = false)
    private Long contaFinanceiraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao", nullable = false, length = 32)
    private TipoMovimentacaoFinanceira tipoMovimentacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_tipo", nullable = false, length = 32)
    private OrigemMovimentacaoFinanceira origemTipo;

    @Column(name = "origem_id")
    private Long origemId;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "documento_referencia", length = 80)
    private String documentoReferencia;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDate dataMovimentacao;

    @Column(name = "data_hora_registro", nullable = false)
    private LocalDateTime dataHoraRegistro;

    @Column(name = "valor_bruto", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_taxa", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTaxa = BigDecimal.ZERO;

    @Column(name = "valor_juros_desconto", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorJurosDesconto = BigDecimal.ZERO;

    @Column(name = "valor_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLiquido;

    @Column(name = "plano_conta_id")
    private Long planoContaId;

    @Column(name = "centro_custo_id")
    private Long centroCustoId;

    @Column(name = "transferencia_par_id")
    private Long transferenciaParId;

    @Column(name = "movimentacao_estornada_id")
    private Long movimentacaoEstornadaId;

    @Column(nullable = false)
    private boolean estornado = false;

    @Column(nullable = false)
    private boolean conciliado = false;

    @Column(name = "conciliacao_item_id")
    private Long conciliacaoItemId;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @Column(name = "criado_por", length = 80)
    private String criadoPor;

    @Version
    @Column(nullable = false)
    private Long version;
}
