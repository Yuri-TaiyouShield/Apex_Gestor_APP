package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "Venda_Devolucao")
@Data
public class VendaDevolucao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venda_devolucao")
    private Long idVendaDevolucao;
    @Column(name = "venda_original_id", nullable = false)
    private Long vendaOriginalId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TipoDevolucao tipo;
    @Column(nullable = false, length = 255)
    private String motivo;
    @Column(name = "valor_ajuste", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAjuste;
    @Column(name = "criado_por", length = 80)
    private String criadoPor;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @OneToMany(mappedBy = "devolucao", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<VendaDevolucaoItem> itens = new ArrayList<>();
}
