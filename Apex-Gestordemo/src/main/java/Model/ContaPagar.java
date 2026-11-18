package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "Conta_Pagar", indexes = {
        @Index(name = "idx_conta_pagar_tenant_status", columnList = "empresa_id, filial_id, status, vencimento"),
        @Index(name = "idx_conta_pagar_tenant_pago", columnList = "empresa_id, filial_id, pago_em"),
        @Index(name = "idx_conta_pagar_tenant_comp", columnList = "empresa_id, filial_id, data_competencia, status"),
        @Index(name = "idx_conta_pagar_classificacao", columnList = "empresa_id, filial_id, plano_conta_id, centro_custo_id")
})
@Data
public class ContaPagar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conta_pagar")
    private Long idContaPagar;
    @Column(name = "fornecedor_id")
    private Long fornecedorId;
    @Column(name = "categoria_id")
    private Long categoriaId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Column(nullable = false, length = 160)
    private String descricao;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;
    @Column(name = "valor_pago", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;
    @Column(nullable = false)
    private LocalDate vencimento;
    @Column(name = "forma_pagamento_prevista_id")
    private Long formaPagamentoPrevistaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FinanceiroStatus status;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @Column(name = "pago_em")
    private LocalDateTime pagoEm;
    @Column(name = "data_competencia")
    private LocalDate dataCompetencia;
    @Column(name = "data_emissao")
    private LocalDate dataEmissao;
    @Column(name = "plano_conta_id")
    private Long planoContaId;
    @Column(name = "centro_custo_id")
    private Long centroCustoId;
    @Column(name = "numero_parcela", nullable = false)
    private Integer numeroParcela = 1;
    @Column(name = "total_parcelas", nullable = false)
    private Integer totalParcelas = 1;
    @Column(name = "grupo_parcelamento", length = 64)
    private String grupoParcelamento;
    @Column(nullable = false)
    private boolean recorrente = false;
    @Column(length = 24)
    private String periodicidade;
    @Column(name = "conta_financeira_id")
    private Long contaFinanceiraId;
    @Version
    @Column(nullable = false)
    private Long version;
}
