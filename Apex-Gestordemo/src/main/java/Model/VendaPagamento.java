package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "Venda_FormasPagamento")
@Data
public class VendaPagamento {

    @ManyToOne
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne
    @JoinColumn(name = "forma_pagamento_id", nullable = false)
    private FormaPagamento formaPagamento;
    
     @Column(name = "valor_pago", precision = 10, scale = 2)
    private BigDecimal valorPago; 

    @Column(name = "numero_parcelas")
    private int numeroParcelas; 
}
