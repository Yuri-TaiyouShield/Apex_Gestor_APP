package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "balanco_patrimonial_gerencial", uniqueConstraints = {
        @UniqueConstraint(name = "uk_balanco_patrimonial_tenant_comp", columnNames = {"empresa_id", "filial_id", "competencia_ano_mes"})
}, indexes = {
        @Index(name = "idx_balanco_patrimonial_tenant_data", columnList = "empresa_id, filial_id, data_referencia")
})
@Data
public class BalancoPatrimonialGerencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_balanco_patrimonial")
    private Long idBalancoPatrimonial;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(name = "competencia_ano_mes", nullable = false, length = 7)
    private String competenciaAnoMes;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "disponibilidades_caixa_bancos", nullable = false, precision = 15, scale = 2)
    private BigDecimal disponibilidadesCaixaBancos = BigDecimal.ZERO;

    @Column(name = "contas_receber_operacionais", nullable = false, precision = 15, scale = 2)
    private BigDecimal contasReceberOperacionais = BigDecimal.ZERO;

    @Column(name = "estoques_operacionais", nullable = false, precision = 15, scale = 2)
    private BigDecimal estoquesOperacionais = BigDecimal.ZERO;

    @Column(name = "adiantamentos_operacionais", nullable = false, precision = 15, scale = 2)
    private BigDecimal adiantamentosOperacionais = BigDecimal.ZERO;

    @Column(name = "despesas_antecipadas", nullable = false, precision = 15, scale = 2)
    private BigDecimal despesasAntecipadas = BigDecimal.ZERO;

    @Column(name = "outros_ativos_circulantes", nullable = false, precision = 15, scale = 2)
    private BigDecimal outrosAtivosCirculantes = BigDecimal.ZERO;

    @Column(name = "realizavel_longo_prazo", nullable = false, precision = 15, scale = 2)
    private BigDecimal realizavelLongoPrazo = BigDecimal.ZERO;

    @Column(name = "investimentos_longo_prazo", nullable = false, precision = 15, scale = 2)
    private BigDecimal investimentosLongoPrazo = BigDecimal.ZERO;

    @Column(name = "imobilizado_bruto", nullable = false, precision = 15, scale = 2)
    private BigDecimal imobilizadoBruto = BigDecimal.ZERO;

    @Column(name = "depreciacao_acumulada", nullable = false, precision = 15, scale = 2)
    private BigDecimal depreciacaoAcumulada = BigDecimal.ZERO;

    @Column(name = "intangivel", nullable = false, precision = 15, scale = 2)
    private BigDecimal intangivel = BigDecimal.ZERO;

    @Column(name = "fornecedores_operacionais_cp", nullable = false, precision = 15, scale = 2)
    private BigDecimal fornecedoresOperacionaisCp = BigDecimal.ZERO;

    @Column(name = "obrigacoes_tributarias_trabalhistas_cp", nullable = false, precision = 15, scale = 2)
    private BigDecimal obrigacoesTributariasTrabalhistasCp = BigDecimal.ZERO;

    @Column(name = "outras_obrigacoes_operacionais_cp", nullable = false, precision = 15, scale = 2)
    private BigDecimal outrasObrigacoesOperacionaisCp = BigDecimal.ZERO;

    @Column(name = "emprestimos_financiamentos_cp", nullable = false, precision = 15, scale = 2)
    private BigDecimal emprestimosFinanciamentosCp = BigDecimal.ZERO;

    @Column(name = "emprestimos_financiamentos_lp", nullable = false, precision = 15, scale = 2)
    private BigDecimal emprestimosFinanciamentosLp = BigDecimal.ZERO;

    @Column(name = "outras_obrigacoes_lp", nullable = false, precision = 15, scale = 2)
    private BigDecimal outrasObrigacoesLp = BigDecimal.ZERO;

    @Column(name = "capital_social", nullable = false, precision = 15, scale = 2)
    private BigDecimal capitalSocial = BigDecimal.ZERO;

    @Column(name = "reservas_capital_lucros", nullable = false, precision = 15, scale = 2)
    private BigDecimal reservasCapitalLucros = BigDecimal.ZERO;

    @Column(name = "lucros_prejuizos_acumulados", nullable = false, precision = 15, scale = 2)
    private BigDecimal lucrosPrejuizosAcumulados = BigDecimal.ZERO;

    @Column(name = "depreciacao_amortizacao_periodo", nullable = false, precision = 15, scale = 2)
    private BigDecimal depreciacaoAmortizacaoPeriodo = BigDecimal.ZERO;

    @Column(name = "amortizacao_principal_divida_periodo", nullable = false, precision = 15, scale = 2)
    private BigDecimal amortizacaoPrincipalDividaPeriodo = BigDecimal.ZERO;

    @Column(name = "sincronizado_operacional", nullable = false)
    private boolean sincronizadoOperacional = true;

    @Column(name = "confirmado_gestor", nullable = false)
    private boolean confirmadoGestor = false;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "atualizado_por", length = 80)
    private String atualizadoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        if (atualizadoEm == null) {
            atualizadoEm = now;
        }
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
