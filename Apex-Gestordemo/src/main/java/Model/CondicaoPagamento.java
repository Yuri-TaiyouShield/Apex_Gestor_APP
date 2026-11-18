package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;

@Entity
@Table(name = "Condicoes_Pagamento")
@Data
public class CondicaoPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_condicao_pagamento")
    private Long idCondicaoPagamento;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "tipo_pagamento", nullable = false, length = 40)
    private String tipoPagamento;

    @Column(name = "quantidade_parcelas", nullable = false)
    private Integer quantidadeParcelas;

    @Column(name = "prazo_dias", nullable = false)
    private Integer prazoDias;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal percentual;

    @Column(length = 255)
    private String restricoes;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "filial_id")
    private Long filialId;
}
