package Model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Despesas", indexes = {
    @Index(name = "idx_despesas_status_vencimento", columnList = "status, data_vencimento"),
    @Index(name = "idx_despesas_tipo", columnList = "tipo_despesa_id")
})
@Data
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despesa")
    private Long idDespesa;
    @Column(nullable = false)
    private String descricao;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;
    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;
    @ManyToOne
    @JoinColumn(name = "tipo_despesa_id", nullable = false)
    private TipoDespesa tipoDespesa;
    @Column(nullable = false)
    private int status;
}
