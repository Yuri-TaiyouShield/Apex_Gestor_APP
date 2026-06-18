package Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Audited
@Table(name = "venda", indexes = {
    @Index(name = "idx_venda_status_data", columnList = "status, data_venda"),
    @Index(name = "idx_venda_cliente", columnList = "cliente_id"),
    @Index(name = "idx_venda_usuario", columnList = "usuario_id")
})
@Data
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venda")
    private Long idVenda;
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    @Column(nullable = false)
    private int status;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacional", length = 24)
    private VendaStatus statusOperacional;
    @Column(name = "numero_venda", length = 40)
    private String numeroVenda;
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;
    @Column(precision = 10, scale = 2)
    private BigDecimal desconto;
    @Column(length = 255)
    private String observacao;
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(name = "filial_id")
    private Long filialId;
    @Column(name = "caixa_sessao_id")
    private Long caixaSessaoId;
    @Column(name = "condicao_pagamento_id")
    private Long condicaoPagamentoId;
    @Column(name = "forma_pagamento_real_id")
    private Long formaPagamentoRealId;
    @Column(name = "valor_recebido", precision = 12, scale = 2)
    private BigDecimal valorRecebido;
    @Column(precision = 12, scale = 2)
    private BigDecimal troco;
    @Column(precision = 12, scale = 2)
    private BigDecimal diferenca;
    @Column(name = "enviado_caixa_em")
    private LocalDateTime enviadoCaixaEm;
    @Column(name = "recebido_caixa_em")
    private LocalDateTime recebidoCaixaEm;
    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;
    @Column(name = "motivo_recusa", length = 255)
    private String motivoRecusa;
    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;
    @Column(name = "venda_original_id")
    private Long vendaOriginalId;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_devolucao", length = 16)
    private TipoDevolucao tipoDevolucao;
    @Column(name = "motivo_devolucao", length = 255)
    private String motivoDevolucao;
    @Column(name = "estoque_movimentado", nullable = false)
    private boolean estoqueMovimentado;
    @Column(name = "data_venda", nullable = false)
    private LocalDateTime dataVenda;
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProdutoVenda> itens;
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<VendaPagamento> pagamentos;
}
