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
@Table(name = "conciliacao_financeira_item", indexes = {
        @Index(name = "idx_conc_fin_tenant_status", columnList = "empresa_id, filial_id, conta_financeira_id, status_conciliacao, data_extrato")
})
@Data
public class ConciliacaoFinanceiraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conciliacao_item")
    private Long idConciliacaoItem;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(name = "conta_financeira_id", nullable = false)
    private Long contaFinanceiraId;

    @Column(name = "tipo_conciliacao", nullable = false, length = 32)
    private String tipoConciliacao;

    @Column(name = "formato_origem", nullable = false, length = 16)
    private String formatoOrigem;

    @Column(name = "identificador_extrato", length = 120)
    private String identificadorExtrato;

    @Column(name = "data_extrato", nullable = false)
    private LocalDate dataExtrato;

    @Column(name = "descricao_extrato", nullable = false, length = 200)
    private String descricaoExtrato;

    @Column(name = "valor_extrato", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorExtrato;

    @Column(name = "natureza_extrato", nullable = false, length = 16)
    private String naturezaExtrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conciliacao", nullable = false, length = 32)
    private StatusConciliacaoFinanceira statusConciliacao;

    @Column(name = "movimentacao_vinculada_id")
    private Long movimentacaoVinculadaId;

    @Column(name = "documento_origem_tipo", length = 32)
    private String documentoOrigemTipo;

    @Column(name = "documento_origem_id")
    private Long documentoOrigemId;

    @Column(name = "diferenca_valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal diferencaValor = BigDecimal.ZERO;

    @Column(length = 255)
    private String observacao;

    @Column(name = "conciliado_por", length = 80)
    private String conciliadoPor;

    @Column(name = "conciliado_em")
    private LocalDateTime conciliadoEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Version
    @Column(nullable = false)
    private Long version;
}
