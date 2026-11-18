package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "parametro_inteligencia_administrativa", uniqueConstraints = {
        @UniqueConstraint(name = "uk_param_inteligencia_tenant", columnNames = {"empresa_id", "filial_id"})
})
@Data
public class ParametroInteligenciaAdministrativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro_inteligencia")
    private Long idParametroInteligencia;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(name = "segmento_negocio", nullable = false, length = 60)
    private String segmentoNegocio = "VAREJO_GERAL";

    @Column(name = "retorno_minimo_mensal_percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal retornoMinimoMensalPercentual = new BigDecimal("1.5000");

    @Column(name = "capital_investido_referencia", nullable = false, precision = 15, scale = 2)
    private BigDecimal capitalInvestidoReferencia = BigDecimal.ZERO;

    @Column(name = "taxa_desconto_anual_percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxaDescontoAnualPercentual = new BigDecimal("12.0000");

    @Column(name = "margem_contribuicao_minima_percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal margemContribuicaoMinimaPercentual = new BigDecimal("25.0000");

    @Column(name = "margem_liquida_minima_percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal margemLiquidaMinimaPercentual = new BigDecimal("8.0000");

    @Column(name = "markup_margem_desejada_padrao", nullable = false, precision = 7, scale = 4)
    private BigDecimal markupMargemDesejadaPadrao = new BigDecimal("20.0000");

    @Column(name = "aliquota_tributos_venda_padrao", nullable = false, precision = 7, scale = 4)
    private BigDecimal aliquotaTributosVendaPadrao = new BigDecimal("8.0000");

    @Column(name = "taxa_cartao_media_padrao", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxaCartaoMediaPadrao = new BigDecimal("2.5000");

    @Column(name = "comissao_venda_padrao", nullable = false, precision = 7, scale = 4)
    private BigDecimal comissaoVendaPadrao = new BigDecimal("2.0000");

    @Column(name = "frete_variavel_venda_padrao", nullable = false, precision = 7, scale = 4)
    private BigDecimal freteVariavelVendaPadrao = BigDecimal.ZERO;

    @Column(name = "ciclo_financeiro_maximo_dias", nullable = false, precision = 10, scale = 2)
    private BigDecimal cicloFinanceiroMaximoDias = new BigDecimal("45.00");

    @Column(name = "pme_maximo_dias", nullable = false, precision = 10, scale = 2)
    private BigDecimal pmeMaximoDias = new BigDecimal("60.00");

    @Column(name = "liquidez_corrente_minima", nullable = false, precision = 10, scale = 4)
    private BigDecimal liquidezCorrenteMinima = new BigDecimal("1.2000");

    @Column(name = "liquidez_seca_minima", nullable = false, precision = 10, scale = 4)
    private BigDecimal liquidezSecaMinima = new BigDecimal("0.9000");

    @Column(name = "liquidez_imediata_minima", nullable = false, precision = 10, scale = 4)
    private BigDecimal liquidezImediataMinima = new BigDecimal("0.3000");

    @Column(name = "endividamento_geral_maximo_percentual", nullable = false, precision = 7, scale = 4)
    private BigDecimal endividamentoGeralMaximoPercentual = new BigDecimal("60.0000");

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
