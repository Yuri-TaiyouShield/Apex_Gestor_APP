package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "regra_classificacao_financeira", indexes = {
        @Index(name = "idx_regra_class_tenant_origem", columnList = "empresa_id, filial_id, origem_tipo, ativo, prioridade")
})
@Data
public class RegraClassificacaoFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regra_classificacao")
    private Long idRegraClassificacao;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_tipo", nullable = false, length = 32)
    private OrigemClassificacaoFinanceira origemTipo;

    @Column(name = "tipo_despesa_id")
    private Long tipoDespesaId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(name = "forma_pagamento_id")
    private Long formaPagamentoId;

    @Column(name = "fornecedor_id")
    private Long fornecedorId;

    @Column(name = "palavra_chave", length = 120)
    private String palavraChave;

    @Column(name = "plano_conta_id", nullable = false)
    private Long planoContaId;

    @Column(name = "centro_custo_id")
    private Long centroCustoId;

    @Column(nullable = false)
    private Integer prioridade = 100;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        if (atualizadoEm == null) {
            atualizadoEm = now;
        }
        if (prioridade == null) {
            prioridade = 100;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        if (prioridade == null) {
            prioridade = 100;
        }
    }
}
