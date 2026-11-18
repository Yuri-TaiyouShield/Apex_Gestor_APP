package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "Conta_Receber", indexes = {
        @Index(name = "idx_conta_receber_tenant_status", columnList = "empresa_id, filial_id, status, vencimento"),
        @Index(name = "idx_conta_receber_venda", columnList = "empresa_id, filial_id, venda_id"),
        @Index(name = "idx_conta_receber_tenant_pago", columnList = "empresa_id, filial_id, pago_em"),
        @Index(name = "idx_conta_receber_tenant_comp", columnList = "empresa_id, filial_id, data_competencia, status"),
        @Index(name = "idx_conta_receber_classificacao", columnList = "empresa_id, filial_id, plano_conta_id, centro_custo_id")
})
@Data
public class ContaReceber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conta_receber")
    private Long idContaReceber;
    @Column(name = "venda_id")
    private Long vendaId;
    @Column(name = "cliente_id")
    private Long clienteId;
    @Column(name = "forma_pagamento_id")
    private Long formaPagamentoId;
    @Column(name = "condicao_pagamento_id")
    private Long condicaoPagamentoId;
    @Column(name = "caixa_sessao_id")
    private Long caixaSessaoId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Column(length = 160)
    private String descricao;
    @Column(name = "valor_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBruto;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto;
    @Column(name = "valor_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquido;
    @Column(name = "valor_pago", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPago;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;
    @Column(nullable = false)
    private LocalDate vencimento;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_recebivel", nullable = false, length = 32)
    private ModalidadeRecebivel modalidadeRecebivel = ModalidadeRecebivel.DIRETO;
    @Column(name = "taxa_percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxaPercentual = BigDecimal.ZERO;
    @Column(name = "valor_taxa", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTaxa = BigDecimal.ZERO;
    @Column(name = "taxa_antecipacao", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxaAntecipacao = BigDecimal.ZERO;
    @Column(name = "data_prevista_repasse")
    private LocalDate dataPrevistaRepasse;
    @Column(nullable = false)
    private boolean antecipado = false;
    @Column(name = "conta_financeira_id")
    private Long contaFinanceiraId;
    @Version
    @Column(nullable = false)
    private Long version;
}
