package Model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "despesas", indexes = {
    @Index(name = "idx_despesas_status_vencimento", columnList = "status, data_vencimento"),
    @Index(name = "idx_despesas_tipo", columnList = "tipo_despesa_id"),
    @Index(name = "idx_despesas_tenant_pagamento", columnList = "empresa_id, filial_id, data_pagamento"),
    @Index(name = "idx_despesas_tenant_comp", columnList = "empresa_id, filial_id, data_competencia, status"),
    @Index(name = "idx_despesas_classificacao", columnList = "empresa_id, filial_id, plano_conta_id, centro_custo_id")
})
@Data
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despesa")
    private Long idDespesa;
    @Column(nullable = false)
    private String descricao;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;
    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(name = "filial_id")
    private Long filialId;
    @ManyToOne
    @JoinColumn(name = "tipo_despesa_id", nullable = false)
    private TipoDespesa tipoDespesa;
    @Column(nullable = false)
    private int status;
    @Column(name = "data_competencia")
    private LocalDate dataCompetencia;
    @Column(name = "data_emissao")
    private LocalDate dataEmissao;
    @Column(name = "plano_conta_id")
    private Long planoContaId;
    @Column(name = "centro_custo_id")
    private Long centroCustoId;
    @Version
    @Column(nullable = false)
    private Long version;
}
