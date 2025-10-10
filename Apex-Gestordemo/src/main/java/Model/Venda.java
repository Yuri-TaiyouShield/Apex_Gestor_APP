package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "Venda")
@Data
public class Venda {
   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venda")
    private Long idVenda;
    
    @Column(nullable = false)
    private int status;
    
    @ManyToOne // Muitas Vendas podem ser feitas por Um Usuário (vendedor)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // Correspondente ao seu campo "vendedor"

    @ManyToOne // Muitas Vendas podem pertencer a Um Cliente
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;
    
    @Column(length = 255)
    private String observacao;
    
    @Column(name = "data_venda", nullable = false)
    private LocalDateTime dataVenda;
    
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProdutoVendaId> itens;

    
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<VendaPagamento> pagamentos;
}
