package Model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Audited
@Table(name = "produtos", indexes = {
    @Index(name = "idx_produtos_status", columnList = "status"),
    @Index(name = "idx_produtos_descricao", columnList = "descricao"),
    @Index(name = "idx_produtos_codigo_barras", columnList = "codigo_barras")
})
@Data
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Long idProduto;
    @Column(nullable = false, length = 60)
    private String descricao;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal custo; // Custo Médio
    @Column(name = "codigo_barras", length = 20)
    private String codigoBarras;
    @Column(name = "codigo_interno", length = 40)
    private String codigoInterno;
    @Column(name = "codigo_original", length = 60)
    private String codigoOriginal;
    @Column(length = 45)
    private String marca;
    @Column(length = 60)
    private String grupo;
    @Column(length = 60)
    private String subgrupo;
    @Column(name = "unidade_medida", length = 10)
    private String unidadeMedida;
    @Column(length = 40)
    private String embalagem;
    @Column(name = "data_aquisicao")
    private LocalDateTime dataAquisicao;
    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;
    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;
    @Column(name = "valor_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorVenda;
    @Column(precision = 10, scale = 2)
    private BigDecimal margem;
    @Column(length = 80)
    private String localizacao;
    @Column(name = "situacao_fiscal", length = 60)
    private String situacaoFiscal;
    @Column(length = 12)
    private String ncm;
    @Column(length = 12)
    private String cfop;
    @Column(precision = 7, scale = 2)
    private BigDecimal icms;
    @Column(precision = 7, scale = 2)
    private BigDecimal ipi;
    @Column(precision = 7, scale = 2)
    private BigDecimal pis;
    @Column(precision = 7, scale = 2)
    private BigDecimal cofins;
    @Column(length = 500)
    private String observacoes;
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(name = "filial_id")
    private Long filialId;
    @Column(nullable = false)
    private int status;

    @Version
    @Column(nullable = false)
    private Long version;
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
}
