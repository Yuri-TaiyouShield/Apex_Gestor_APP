package Model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "b2c_cart_item", uniqueConstraints = {
        @UniqueConstraint(name = "unq_b2c_cart_item_produto", columnNames = {"cart_id", "produto_id"})
})
@Data
public class B2cCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_b2c_cart_item")
    private Long idB2cCartItem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private B2cCart cart;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitarioSnapshot;
}
