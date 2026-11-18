package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;

@Entity
@Table(name = "Venda_Devolucao_Item")
@Data
public class VendaDevolucaoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venda_devolucao_item")
    private Long idVendaDevolucaoItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucao_id", nullable = false)
    private VendaDevolucao devolucao;
    @Column(name = "produto_original_id", nullable = false)
    private Long produtoOriginalId;
    @Column(name = "produto_novo_id")
    private Long produtoNovoId;
    @Column(nullable = false)
    private Integer quantidade;
    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;
    @Column(name = "custo_unitario", precision = 15, scale = 4)
    private BigDecimal custoUnitario;
}
